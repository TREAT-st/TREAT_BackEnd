package com.example.demo.api.stockDailyPrice.controller;

import com.example.demo.api.common.dto.ApiResponseDto;
import com.example.demo.api.stockDailyPrice.dto.StockDailyPriceResponseDto.BackfillTriggerResponse;
import com.example.demo.api.stockDailyPrice.dto.StockDailyPriceResponseDto.DailyTriggerResponse;
import com.example.demo.api.stockDailyPrice.dto.StockDailyPriceResponseDto.DeleteResponse;
import com.example.demo.api.stockDailyPrice.mapper.StockDailyPriceConverter;
import com.example.demo.api.stockDailyPrice.service.DailyPriceJobGuard;
import com.example.demo.api.stockDailyPrice.service.StockDailyPriceBackfillUseCase.BackfillPlan;
import com.example.demo.api.stockDailyPrice.service.StockDailyPriceBackfillUseCase;
import com.example.demo.api.stockDailyPrice.service.StockDailyPriceDeleteUseCase;
import com.example.demo.api.stockDailyPrice.service.StockDailyPriceSyncUseCase.DailyPlan;
import com.example.demo.api.stockDailyPrice.service.StockDailyPriceSyncUseCase;
import com.example.demo.domain.stockDailyPrice.entity.StockDailyPriceDeleteResult;
import com.example.demo.security.batch.BatchSecretVerifier;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static com.example.demo.common.consts.StaticVariable.BATCH_SECRET_HEADER;
import static com.example.demo.common.consts.StaticVariable.DAILY_PRICE_ALREADY_RUNNING;
import static com.example.demo.common.consts.StaticVariable.DAILY_PRICE_BACKFILL_ACCEPTED;
import static com.example.demo.common.consts.StaticVariable.DAILY_PRICE_DAILY_ACCEPTED;
import static com.example.demo.common.consts.StaticVariable.DAILY_PRICE_NOTHING_TO_FILL;
import static com.example.demo.common.consts.StaticVariable.DAILY_PRICE_REFILL_HINT;
import static com.example.demo.common.consts.StaticVariable.DAILY_PRICE_BACKFILL_NO_TARGET;
import static com.example.demo.common.consts.StaticVariable.DAILY_PRICE_SYNC_LOOKBACK_DAYS;
import static com.example.demo.common.consts.StaticVariable.DAILY_PRICE_SYNC_MAX_ATTEMPT;

/**
 * 차트 일봉 운영 API. 일일 배치 체인과는 별개 작업이라 BatchController와 나눴다.
 *   - /backfill : 3년치 받기 (처음 한 번, 신규 편입, 실패 종목, 삭제 후 다시 채우기)
 *   - /daily    : 하루치 받기. 매일 별도 스케줄러(EventBridge 등)가 일일 배치 완료 후 호출한다
 *   - DELETE    : 기간 삭제
 *
 * 경로는 /api/v1/batch 아래에 둔다. SecurityConfig가 이 경로만 JWT 없이 열어두고,
 * 대신 모든 엔드포인트가 X-Batch-Secret을 검증한다. 다른 경로로 옮기면 보안 설정에 허용 경로를 따로 열어야 한다.
 */
@Tag(name = "[자동화] 배치 - 차트 일봉 API (일일 적재는 자동, 백필·삭제는 수동)")
@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/batch/daily-prices")
@RequiredArgsConstructor
public class StockDailyPriceBatchController {

    private final StockDailyPriceBackfillUseCase stockDailyPriceBackfillUseCase;
    private final StockDailyPriceSyncUseCase stockDailyPriceSyncUseCase;
    private final StockDailyPriceDeleteUseCase stockDailyPriceDeleteUseCase;
    private final DailyPriceJobGuard dailyPriceJobGuard;
    private final BatchSecretVerifier batchSecretVerifier;

    @Operation(summary = "일봉 3년치 백필 (수동)", description =
            "종목별 일봉을 기간으로 받아 저장합니다. 이미 있는 날짜는 덮어씁니다(여러 번 실행해도 결과가 같습니다).<br>" +
                    "처음 한 번, 새로 편입된 종목, 실패 종목 재시도, 삭제 후 다시 채울 때 사용합니다.<br><br>" +
                    "기간은 양 끝 포함입니다. to를 비우면 시세가 반영된 가장 최근 거래일, from을 비우면 to로부터 3년 전입니다. " +
                    "날짜 형식은 \"yyyy-MM-dd\"입니다.<br>" +
                    "to가 최근 거래일보다 뒤거나 from이 to보다 뒤면 400(4255), 반영된 시세가 아직 없으면 409(4256)입니다.<br>" +
                    "stockCodes를 비우면 전 종목(편출 포함)입니다. 지정하면 중복은 하나로 합칩니다. " +
                    "6자리 숫자가 아니거나 없는 종목코드가 하나라도 있으면 요청 전체를 400(4257)으로 거절합니다.<br><br>" +
                    "수 분 걸릴 수 있어 접수만 하고 202로 즉시 응답합니다. 결과는 서버 로그(\"일봉 백필 종료\")로 확인하세요. " +
                    "실패 종목은 stockCodes로 다시 백필하면 됩니다.<br>" +
                    "일일 배치와는 다른 실행기라 서로 막지 않습니다. 다른 일봉 적재(백필·일일)가 어느 서버에서든 돌고 있으면 accepted=false입니다.<br>" +
                    "X-Batch-Secret 헤더에 BATCH_TRIGGER_SECRET 환경 변수 값을 넣어주세요.")
    @PostMapping("/backfill")
    public ResponseEntity<ApiResponseDto<BackfillTriggerResponse>> triggerBackfill(
            @RequestHeader(value = BATCH_SECRET_HEADER, required = false) String secret,
            @RequestParam(required = false) List<String> stockCodes,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to) {
        batchSecretVerifier.verify(secret);

        // 대상 확정과 검증은 비동기로 넘기기 전에 한다. 안에서 하면 잘못된 요청도 202로 접수된다.
        BackfillPlan plan = stockDailyPriceBackfillUseCase.plan(stockCodes, from, to);
        if (plan.isEmpty()) {
            return accepted(StockDailyPriceConverter.toBackfillTriggerResponse(true, DAILY_PRICE_BACKFILL_NO_TARGET, plan));
        }
        Optional<String> lockToken = dailyPriceJobGuard.tryAcquire();
        if (lockToken.isEmpty()) {
            return accepted(StockDailyPriceConverter.toBackfillTriggerResponse(false, DAILY_PRICE_ALREADY_RUNNING, plan));
        }
        try {
            stockDailyPriceBackfillUseCase.backfillAsync(plan, lockToken.get());
        } catch (TaskRejectedException e) {
            dailyPriceJobGuard.release(lockToken.get());
            log.info("다른 일봉 적재가 실행 중이라 백필을 접수하지 않았습니다.");
            return accepted(StockDailyPriceConverter.toBackfillTriggerResponse(false, DAILY_PRICE_ALREADY_RUNNING, plan));
        }

        log.info("일봉 백필 접수. from={} to={} 종목={}", plan.from(), plan.to(), plan.stockCodes().size());
        return accepted(StockDailyPriceConverter.toBackfillTriggerResponse(true, DAILY_PRICE_BACKFILL_ACCEPTED, plan));
    }

    @Operation(summary = "일봉 일일 적재 (자동화)", description =
            "거래일 하루치 일봉을 전 종목 한 번에 받아 저장합니다. 매일 스케줄러가 호출합니다.<br><br>" +
                    "<b>date 생략</b> : 거래일별 적재 이력으로 받을 날짜를 정합니다.<br>" +
                    "· 최근 " + DAILY_PRICE_SYNC_LOOKBACK_DAYS + "일(장애가 더 길었으면 끝난 마지막 날짜 다음 날부터) 안의 평일 중 이력이 없는 날 → 전 종목<br>" +
                    "· PARTIAL·FAILED 날짜(시도 " + DAILY_PRICE_SYNC_MAX_ATTEMPT + "회 미만) → 기록된 실패 종목만<br>" +
                    "· 이력이 하나도 없으면 일봉 테이블의 마지막 날짜 다음 날부터, 일봉도 없으면 최근 거래일 하루만 받습니다(과거는 /backfill).<br>" +
                    "· 받을 날짜가 없으면 실행하지 않고 accepted=true, tradeDates=[]로 응답합니다.<br>" +
                    "<b>date 지정</b> : 그 날짜만 전 종목으로 다시 받습니다. 최근 거래일보다 뒤면 400(4255).<br><br>" +
                    "거래정지·미상장 종목은 실패가 아닌 건너뜀으로 기록됩니다. 휴장일은 HOLIDAY로 기록됩니다.<br>" +
                    "시도 " + DAILY_PRICE_SYNC_MAX_ATTEMPT + "회를 채운 PARTIAL·FAILED 날짜는 자동으로 다시 받지 않습니다. date를 지정하거나 해당 종목을 백필하세요.<br><br>" +
                    "<b>스케줄</b> : 최근 거래일은 일일 배치가 반영한 값이므로, 일일 배치 완료 후 충분한 간격을 두고 호출하세요. " +
                    "배치보다 먼저 호출되거나 배치가 늦어지거나 실패하면 이번 호출은 할 일이 없거나 전날까지만 받습니다. " +
                    "오류가 아니며, 이력이 남지 않은 날은 다음 호출이 따라잡습니다.<br>" +
                    "접수만 하고 202로 즉시 응답합니다. 결과는 서버 로그(\"일봉 일일 적재 종료\")와 적재 이력으로 확인하세요.<br>" +
                    "백필이나 다른 일일 적재가 어느 서버에서든 돌고 있으면 accepted=false입니다. 이 경우도 다음 호출이 따라잡습니다.<br>" +
                    "X-Batch-Secret 헤더에 BATCH_TRIGGER_SECRET 환경 변수 값을 넣어주세요.")
    @PostMapping("/daily")
    public ResponseEntity<ApiResponseDto<DailyTriggerResponse>> triggerDaily(
            @RequestHeader(value = BATCH_SECRET_HEADER, required = false) String secret,
            @RequestParam(required = false) LocalDate date) {
        batchSecretVerifier.verify(secret);

        DailyPlan plan = stockDailyPriceSyncUseCase.plan(date);
        if (plan.isEmpty()) {
            log.info("받을 일봉이 없어 일일 적재를 실행하지 않습니다. 최근거래일={}", plan.latestTradeDate());
            return accepted(StockDailyPriceConverter.toDailyTriggerResponse(true, DAILY_PRICE_NOTHING_TO_FILL, plan));
        }
        Optional<String> lockToken = dailyPriceJobGuard.tryAcquire();
        if (lockToken.isEmpty()) {
            return accepted(StockDailyPriceConverter.toDailyTriggerResponse(false, DAILY_PRICE_ALREADY_RUNNING, plan));
        }
        try {
            stockDailyPriceSyncUseCase.runAsync(plan, lockToken.get());
        } catch (TaskRejectedException e) {
            dailyPriceJobGuard.release(lockToken.get());
            log.info("다른 일봉 적재가 실행 중이라 일일 적재를 접수하지 않았습니다.");
            return accepted(StockDailyPriceConverter.toDailyTriggerResponse(false, DAILY_PRICE_ALREADY_RUNNING, plan));
        }

        log.info("일봉 일일 적재 접수. 날짜={}", plan.tradeDates());
        return accepted(StockDailyPriceConverter.toDailyTriggerResponse(true, DAILY_PRICE_DAILY_ACCEPTED, plan));
    }

    @Operation(summary = "일봉 기간 삭제 (수동)", description =
            "기간의 일봉을 지우고 지운 행 수를 돌려줍니다. 기간은 양 끝 포함이며 from·to는 필수입니다. from이 to보다 뒤면 400(4255).<br>" +
                    "stockCode를 비우면 전 종목을 지우고, 그 기간의 일일 적재 이력도 함께 지웁니다. " +
                    "지정하면 그 종목만 지우고 이력은 그대로 둡니다. 없는 종목이면 404(4250)입니다.<br>" +
                    "백필이나 일일 적재가 어느 서버에서든 돌고 있으면 409(4258)로 거절합니다. 적재가 끝난 뒤 다시 요청하세요.<br>" +
                    "<b>다시 채우기</b><br>" +
                    "· 전 종목을 최근 " + DAILY_PRICE_SYNC_LOOKBACK_DAYS + "일 안에서 지웠으면 이력도 함께 지워져 다음 /daily가 다시 받습니다.<br>" +
                    "· 그보다 오래된 구간이나 한 종목만 지웠으면 자동으로 다시 채워지지 않습니다. " +
                    "/backfill을 같은 기간(한 종목이면 stockCodes)으로 호출하세요.<br>" +
                    "X-Batch-Secret 헤더에 BATCH_TRIGGER_SECRET 환경 변수 값을 넣어주세요.")
    @DeleteMapping
    public ApiResponseDto<DeleteResponse> deleteDailyPrices(
            @RequestHeader(value = BATCH_SECRET_HEADER, required = false) String secret,
            @RequestParam LocalDate from,
            @RequestParam LocalDate to,
            @RequestParam(required = false)
            @Pattern(regexp = "\\d{6}", message = "종목코드는 6자리 숫자입니다.") String stockCode) {
        batchSecretVerifier.verify(secret);

        StockDailyPriceDeleteResult result = stockDailyPriceDeleteUseCase.delete(from, to, stockCode);
        return ApiResponseDto.onSuccess(
                StockDailyPriceConverter.toDeleteResponse(from, to, stockCode, result, DAILY_PRICE_REFILL_HINT));
    }

    private static <T> ResponseEntity<ApiResponseDto<T>> accepted(T body) {
        return ResponseEntity.accepted().body(ApiResponseDto.onSuccess(body));
    }
}
