package com.example.demo.api.batch.service;

import com.example.demo.api.batch.dto.BatchResponseDto.VolatilityDailyBatchResult;
import com.example.demo.api.batch.mapper.BatchConverter;
import com.example.demo.api.stock.dto.StockResponseDto.SyncStocksResponse;
import com.example.demo.api.stock.service.StockUseCase;
import com.example.demo.api.volatility.dto.VolatilityResponseDto.DetectionResult;
import com.example.demo.api.volatility.dto.VolatilityResponseDto.ReportGenerationResult;
import com.example.demo.api.volatility.service.VolatilityUseCase;
import com.example.demo.common.annotation.UseCase;
import com.example.demo.domain.batch.entity.BatchStartResult;
import com.example.demo.domain.batch.entity.BatchStartDecision;
import com.example.demo.domain.batch.entity.BatchStep;
import com.example.demo.domain.batch.service.BatchExecutionLogService;
import com.example.demo.domain.stock.entity.Kospi200SyncCommand;
import com.example.demo.domain.volatility.service.VolatilityQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.function.Supplier;

import static com.example.demo.api.volatility.dto.VolatilityRequestDto.ReportGenerationRequest;
import static com.example.demo.common.consts.StaticVariable.ANOTHER_RUN_IN_PROGRESS;
import static com.example.demo.common.consts.StaticVariable.NO_DETECTED_STOCK;
import static com.example.demo.common.consts.StaticVariable.SEOUL_ZONE;

/**
 * 일일 배치 체인. SYNC -> DETECT -> REPORT를 순서대로 실행하고 각 단계를 실행 이력에 남긴다.
 * 마지막으로 리포트 도착을 확인할 VERIFY를 열어두고 끝낸다.
 *
 * 클래스 레벨 @Transactional을 붙이지 않는다.
 * 각 단계가 수 분짜리 Lambda 호출을 포함하므로 그동안 DB 커넥션을 붙잡으면 안 된다.
 * 저장은 하위 커맨드 서비스가 각자의 트랜잭션으로 처리한다.
 */
@Slf4j
@UseCase
@RequiredArgsConstructor
public class VolatilityDailyBatchUseCase {

    private final StockUseCase stockUseCase;
    private final VolatilityUseCase volatilityUseCase;
    private final VolatilityQueryService volatilityQueryService;
    private final BatchExecutionLogService batchExecutionLogService;

    /**
     * @param force 이미 성공한 단계도 다시 실행한다. 리포트 단계는 GPT 비용이 다시 나가므로
     *              운영자가 의도적으로 되돌릴 때만 쓴다.
     */
    public VolatilityDailyBatchResult runDailyBatch(boolean force) {
        // 거래일과 동기화 대상을 한 응답에서 받는다. 나눠 부르면 판정에 쓴 거래일과
        // 실제로 저장하는 데이터의 거래일이 갈릴 수 있다.
        //
        // 여기서 터지면 거래일을 모르므로 실행 이력에 남길 키가 없다. 그래도 던지지는 않는다.
        // 체인은 비동기로 돌 예정이라 예외를 밖으로 보내면 받을 곳이 없어 그대로 사라진다.
        Kospi200SyncCommand kospi200;
        try {
            kospi200 = stockUseCase.fetchKospi200();
        } catch (Exception e) {
            log.error("거래일을 확보하지 못해 배치를 시작하지 못했습니다.", e);
            return BatchConverter.toAbortedResult(null, null, summarizeFailure(e));
        }
        LocalDate tradeDate = kospi200.tradeDate();

        if (isNotTradingDay(tradeDate)) {
            return BatchConverter.toHolidayResult(tradeDate);
        }

        StepResult<Void> syncStep = runStep(tradeDate, BatchStep.SYNC, force, () -> {
            SyncStocksResponse response = stockUseCase.syncKospi200(kospi200);
            return new StepOutput<>(null, summarize(response));
        });
        if (!syncStep.canContinue()) {
            return BatchConverter.toAbortedResult(tradeDate, BatchStep.SYNC, syncStep.haltReason());
        }

        StepResult<Integer> detectStep = runStep(tradeDate, BatchStep.DETECT, force, () -> {
            DetectionResult result = volatilityUseCase.runDetection();
            return new StepOutput<>(result.getDetectedCount(), summarize(result));
        });
        if (!detectStep.canContinue()) {
            return BatchConverter.toAbortedResult(tradeDate, BatchStep.DETECT, detectStep.haltReason());
        }

        if (!hasReportTargets(tradeDate, detectStep.output())) {
            log.info("탐지된 종목이 없어 리포트 생성을 건너뜁니다. tradeDate={}", tradeDate);
            batchExecutionLogService.skip(tradeDate, BatchStep.REPORT, NO_DETECTED_STOCK);
            return BatchConverter.toCompletedResult(tradeDate);
        }

        StepResult<Void> reportStep = runStep(tradeDate, BatchStep.REPORT, force, () -> {
            ReportGenerationResult result =
                    volatilityUseCase.runReportGeneration(new ReportGenerationRequest(), tradeDate);
            if (result.getFailedCount() > 0) {
                log.warn("리포트 생성 요청이 일부 실패했습니다. 도착 확인 단계에서 다시 봐야 합니다. "
                                + "tradeDate={} 실패={}건 종목={}",
                        tradeDate, result.getFailedCount(), result.getFailedStockCodes());
            }
            return new StepOutput<>(null, summarize(result));
        });
        if (!reportStep.canContinue()) {
            return BatchConverter.toAbortedResult(tradeDate, BatchStep.REPORT, reportStep.haltReason());
        }

        openVerifyStep(tradeDate, force);
        return BatchConverter.toCompletedResult(tradeDate);
    }

    /**
     * 오늘이 거래일인지 판정한다.
     *
     * 거래일은 서버 시계가 아니라 KRX가 알려준다. 휴장일에는 직전 거래일을 돌려주므로
     * 그 값이 오늘과 다르면 오늘은 장이 열리지 않은 날이다.
     *
     * 이때는 아무것도 기록하지 않는다. KRX가 준 그 거래일은 이미 처리가 끝난 날이라
     * 건드리면 지난 성공 이력이 덮인다. 휴장은 오늘의 사건이지 특정 거래일의 사건이 아니라
     * 실행 이력에 남길 키가 없다.
     */
    private boolean isNotTradingDay(LocalDate tradeDate) {
        LocalDate today = LocalDate.now(SEOUL_ZONE);
        if (tradeDate.isEqual(today)) {
            return false;
        }

        log.info("거래일이 아니라 실행하지 않습니다. tradeDate={} today={}", tradeDate, today);
        return true;
    }

    /**
     * 리포트 도착 확인 단계를 열어둔다.
     *
     * 체인이 실행하는 단계가 아니라 넘겨주는 단계다. 리포트는 ECS가 만들어 콜백으로 알려주므로
     * 체인이 끝난 뒤에야 도착한다. 닫는 일은 Phase 4가 맡는다.
     *
     * 그래서 다른 단계와 달리 결과로 체인을 중단하지 않는다.
     * IN_PROGRESS는 앞선 실행이 정상적으로 기다리는 중이라는 뜻이고,
     * ALREADY_DONE은 이미 전부 도착했다는 뜻이다. 둘 다 문제가 아니다.
     */
    private void openVerifyStep(LocalDate tradeDate, boolean force) {
        BatchStartResult startResult = batchExecutionLogService.tryStart(tradeDate, BatchStep.VERIFY, force);

        if (startResult.decision() == BatchStartDecision.STARTED) {
            log.info("리포트 도착 확인을 시작합니다. tradeDate={}", tradeDate);
            return;
        }
        log.info("리포트 도착 확인을 새로 열지 않습니다. tradeDate={} status={}", tradeDate, startResult.decision());
    }

    /**
     * 리포트를 만들 대상이 있는지 판정한다. 근거는 "가장 최근에 내려진 판단"이다.
     *
     * DETECT를 이번에 돌렸으면 그 결과가 최신이고, 건너뛴 재실행에서는 반환값이 없으므로
     * 지난 실행이 남긴 DB가 최신이다.
     *
     * 둘이 갈릴 수 있다. 탐지가 0건이면 저장 단계가 기존 기록을 지우지 않고 유지하기 때문이다.
     * 콜백으로 채워진 reportUrl을 잃지 않으려는 가드라 그 동작 자체는 옳지만,
     * 그 상태에서 DB만 보면 최신 탐지가 제외한 종목으로 리포트를 요청하게 된다.
     * 되돌릴 수 없는 링크 유실보다는 낫지만 GPT 비용이 헛나가므로, 판정은 최신 탐지를 따른다.
     *
     * @param detectedCount 이번 실행에서 DETECT가 돌지 않았으면 null
     */
    private boolean hasReportTargets(LocalDate tradeDate, Integer detectedCount) {
        if (detectedCount == null) {
            return !volatilityQueryService.getByTradeDate(tradeDate).isEmpty();
        }

        if (detectedCount == 0 && !volatilityQueryService.getByTradeDate(tradeDate).isEmpty()) {
            log.warn("탐지 0건이지만 해당 거래일에 기존 기록이 남아 있습니다. "
                    + "최신 탐지를 따라 리포트 생성을 건너뜁니다. tradeDate={}", tradeDate);
        }
        return detectedCount > 0;
    }

    /**
     * 시작 권한 확인 - 실행 - 성공/실패 기록의 한 사이클.
     * 단계마다 다른 건 본문뿐이라 나머지를 여기 모은다.
     *
     * 예외는 기록하고 삼킨다. 체인은 비동기로 돌 예정이라 던져도 받을 곳이 없고,
     * 중단 사유는 반환값으로 나간다.
     */
    private <T> StepResult<T> runStep(LocalDate tradeDate, BatchStep step, boolean force,
                                      Supplier<StepOutput<T>> stepBody) {
        BatchStartResult startResult = batchExecutionLogService.tryStart(tradeDate, step, force);

        // 이미 끝난 단계다. 실행하지 않지만 체인은 다음 단계로 가야 한다.
        if (startResult.decision() == BatchStartDecision.ALREADY_DONE) {
            return StepResult.alreadyDone();
        }
        // 살아 있는 실행이 있다. 여기서 멈추지 않으면 같은 단계가 두 번 돈다.
        if (startResult.decision() == BatchStartDecision.IN_PROGRESS) {
            log.warn("다른 실행이 진행 중이라 체인을 중단합니다. tradeDate={} step={}", tradeDate, step);
            return StepResult.halted(ANOTHER_RUN_IN_PROGRESS);
        }

        try {
            StepOutput<T> output = stepBody.get();
            batchExecutionLogService.succeed(startResult.execution(), output.summary());
            return StepResult.executed(output.value());
        } catch (Exception e) {
            log.error("배치 단계 실패. tradeDate={} step={}", tradeDate, step, e);
            String failure = summarizeFailure(e);
            batchExecutionLogService.fail(startResult.execution(), failure);
            return StepResult.halted(failure);
        }
    }

    /**
     * 예외 메시지가 없는 경우가 있다. 인자 없이 던진 예외나 일부 런타임 예외가 그렇다.
     * 그대로 두면 실행 이력에 FAILED만 남고 사유가 비어 무슨 일이었는지 알 수 없다.
     *
     * 도메인 예외(GeneralException)는 getMessage를 오버라이드해 사유 문구를 돌려주므로 그대로 쓰인다.
     */
    private String summarizeFailure(Exception e) {
        return e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
    }

    private String summarize(SyncStocksResponse response) {
        return "추가 %d, 갱신 %d, 편출 %d, 재편입 %d, 시세 %d건".formatted(
                response.getAddedCount(), response.getUpdatedCount(), response.getDeactivatedCount(),
                response.getReactivatedCount(), response.getPriceUpdatedCount());
    }

    private String summarize(DetectionResult result) {
        return "탐지 %d건".formatted(result.getDetectedCount());
    }

    /**
     * "요청"이라고 적는 이유가 있다. 이 단계의 성공은 리포트 생성 요청이 나갔다는 뜻이지
     * 파일이 만들어졌다는 뜻이 아니다. 실제 생성과 reportUrl 저장은 콜백으로 나중에 일어난다.
     *
     * 일부 종목의 요청이 실패해도 단계는 성공이다. 실패를 이유로 FAILED를 남기면 재실행이
     * 이미 요청한 종목까지 다시 요청해 GPT 비용이 두 배가 된다. 못 나간 요청은 도착 확인 단계가
     * 미도착으로 잡아 그 종목만 다시 보낸다. 대신 어느 종목이 못 나갔는지는 여기 남겨둔다.
     */
    private String summarize(ReportGenerationResult result) {
        if (result.getFailedCount() == 0) {
            return "생성 요청 %d건".formatted(result.getRequestedCount());
        }
        return "생성 요청 %d건, 요청 실패 %d건 %s".formatted(
                result.getRequestedCount(), result.getFailedCount(), result.getFailedStockCodes());
    }

    /** 단계 본문이 만들어낸 값과, 실행 이력에 남길 한 줄 요약. */
    private record StepOutput<T>(T value, String summary) {
    }

    /**
     * 한 단계를 거친 뒤 체인이 이어갈 수 있는지와, 그 단계가 남긴 값.
     *
     * 이미 성공한 단계(alreadyDone)는 실행하지 않았지만 다음 단계로 가야 하므로 canContinue가 참이다.
     * "실행하지 않았다"와 "멈춰야 한다"를 뭉뚱그리면 겹친 실행이 리포트까지 도달한다.
     *
     * @param output     실행하지 않았으면 null이다. 판정에 쓸 때 그 차이를 봐야 한다.
     * @param haltReason 멈춘 이유. 이어갈 수 있으면 null이다.
     */
    private record StepResult<T>(boolean canContinue, T output, String haltReason) {

        static <T> StepResult<T> executed(T output) {
            return new StepResult<>(true, output, null);
        }

        static <T> StepResult<T> alreadyDone() {
            return new StepResult<>(true, null, null);
        }

        static <T> StepResult<T> halted(String haltReason) {
            return new StepResult<>(false, null, haltReason);
        }
    }
}
