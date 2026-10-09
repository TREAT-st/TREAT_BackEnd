package com.example.demo.api.volatility.service;

import com.example.demo.api.report.client.ReportLambdaClient;
import com.example.demo.api.report.dto.ReportLambdaRequestDto;
import com.example.demo.api.volatility.dto.VolatilityResponseDto.DetectionResult;
import com.example.demo.api.volatility.dto.VolatilityResponseDto.ReportGenerationResult;
import com.example.demo.api.volatility.dto.VolatilityResponseDto.VolatilityListResponse;
import com.example.demo.api.volatility.mapper.VolatilityConverter;
import com.example.demo.common.annotation.UseCase;
import com.example.demo.domain.volatility.entity.Volatility;
import com.example.demo.domain.volatility.entity.VolatilityDetectionResult;
import com.example.demo.domain.volatility.exception.VolatilityHandler;
import com.example.demo.domain.volatility.service.VolatilityCommandService;
import com.example.demo.domain.volatility.service.VolatilityDetectionService;
import com.example.demo.domain.batch.entity.BatchStep;
import com.example.demo.domain.batch.service.BatchExecutionLogService;
import com.example.demo.domain.volatility.service.VolatilityQueryService;
import com.example.demo.domain.volatility.entity.VolatilitySignal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;

import static com.example.demo.api.volatility.dto.VolatilityRequestDto.ReportCallback;
import static com.example.demo.api.volatility.dto.VolatilityRequestDto.ReportGenerationRequest;
import static com.example.demo.api.volatility.dto.VolatilityRequestDto.SingleReportRequest;
import static com.example.demo.common.consts.StaticVariable.VERIFY_ALL_REPORTS_ARRIVED;

@Slf4j
@UseCase
@Transactional
@RequiredArgsConstructor
public class VolatilityUseCase {
    private final VolatilityQueryService volatilityQueryService;
    private final VolatilityDetectionService volatilityDetectionService;
    private final VolatilityCommandService volatilityCommandService;
    private final ReportLambdaClient reportLambdaClient;
    private final BatchExecutionLogService batchExecutionLogService;

    /**
     * yyyyMMdd. 기본 SMART 해석은 20260231 같은 날짜를 2월 말로 보정해버려서,
     * 콜백이 엉뚱한 거래일의 행을 갱신할 수 있다. STRICT로 거부한다.
     * STRICT에서는 연도 필드로 yyyy(연호 기준) 대신 uuuu(proleptic)를 써야 한다.
     */
    private static final DateTimeFormatter REPORT_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("uuuuMMdd").withResolverStyle(ResolverStyle.STRICT);

    /** KRX Lambda에 요청할 시총 상위 종목 수. */
    private static final int DETECTION_REQUEST_SIZE = 100;
    /** 저장할 상위 변동성 종목 수. */
    private static final int DETECTION_TOP_N = 10;

    /**
     * Lambda 호출이 수 분 걸리므로 트랜잭션 밖에서 실행한다.
     * 저장은 VolatilityCommandService가 자체 트랜잭션으로 처리한다.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public DetectionResult runDetection() {
        return detectAndSave(null);
    }

    /**
     * 배치 경로. SYNC가 확정한 거래일과 탐지 결과의 거래일이 같아야만 저장한다.
     *
     * 탐지는 시세 동기화와 다른 Lambda가 거래일을 정한다. 두 Lambda의 거래일 규칙이 어긋나면
     * 탐지 결과가 다른 날짜로 저장되고, 뒤이은 REPORT는 SYNC 거래일로 대상을 찾으므로
     * 0건으로 조용히 끝난다. 반환값을 받은 뒤 비교하면 이미 저장된 다음이라 늦다.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public DetectionResult runDetection(LocalDate expectedTradeDate) {
        return detectAndSave(expectedTradeDate);
    }

    /** @param expectedTradeDate null이면 거래일을 검증하지 않는다(수동 실행). */
    private DetectionResult detectAndSave(LocalDate expectedTradeDate) {
        VolatilityDetectionResult detection = volatilityDetectionService.detect(DETECTION_REQUEST_SIZE);

        if (expectedTradeDate != null && !expectedTradeDate.isEqual(detection.tradeDate())) {
            log.error("탐지 결과의 거래일이 동기화 거래일과 다릅니다. 저장하지 않습니다. "
                    + "expected={} detected={}", expectedTradeDate, detection.tradeDate());
            throw VolatilityHandler.tradeDateMismatch();
        }

        if (detection.signals().isEmpty()) {
            log.error("변동성 분석 결과가 비어 있습니다. 전 종목이 스킵됐습니다.");
            throw VolatilityHandler.volatilityDetectionFailed();
        }

        // 시총 가중치의 분모는 요청 수가 아니라 Lambda가 알려준 실제 유니버스 크기를 쓴다.
        List<VolatilitySignal> topSignals = volatilityDetectionService.selectTop(
                detection.signals(), detection.universeSize(), DETECTION_TOP_N);
        if (topSignals.isEmpty()) {
            log.warn("변동성 알림이 탐지된 종목이 없습니다. 분석 종목 수={}", detection.signals().size());
        }

        volatilityCommandService.saveTopVolatilityStocks(topSignals, detection.tradeDate());
        return VolatilityConverter.toDetectionResult(topSignals, detection.tradeDate());
    }

    /**
     * 수동 실행 경로. 탐지 기록이 있는 가장 최근 거래일을 대상으로 삼는다.
     * getLatestVolatility()는 최신 N건이 아니라 최신 거래일의 전체 목록을 돌려준다.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ReportGenerationResult runReportGeneration(ReportGenerationRequest request) {
        List<Volatility> targets = volatilityQueryService.getLatestVolatility();
        if (targets.isEmpty()) {
            throw VolatilityHandler.volatilityNotDetected();
        }

        LocalDate tradeDate = targets.get(0).getTradeDate();
        return generateReports(request, targets, tradeDate);
    }

    /**
     * 배치 경로. 체인이 확보한 거래일을 그대로 쓴다.
     * 여기서 "최신"으로 다시 해석하면 재실행이나 자정 경계에서 체인이 판정한 날짜와 어긋난다.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ReportGenerationResult runReportGeneration(ReportGenerationRequest request, LocalDate tradeDate) {
        List<Volatility> targets = volatilityQueryService.getByTradeDate(tradeDate);
        if (targets.isEmpty()) {
            throw VolatilityHandler.volatilityNotDetected();
        }

        return generateReports(request, targets, tradeDate);
    }

    /**
     * 리포트 날짜는 저장된 거래일을 그대로 따른다. 서버 날짜를 쓰면 휴장일이나
     * 자정 경계에서 콜백이 조회할 행과 어긋난다.
     *
     * 종목 하나가 실패해도 나머지는 진행한다. 실패 목록은 결과에 담아 돌려준다.
     */
    private ReportGenerationResult generateReports(ReportGenerationRequest request,
                                                   List<Volatility> targets, LocalDate tradeDate) {
        String reportDate = tradeDate.format(REPORT_DATE_FORMATTER);

        List<String> failedStockCodes = new ArrayList<>();
        for (Volatility v : targets) {
            try {
                invokeReportJob(reportDate, v.getStockCode(), v.getStockName(), request.getGptModel());
            } catch (Exception e) {
                log.error("리포트 생성 요청 실패, 다음 종목으로 진행. stockCode={}", v.getStockCode(), e);
                failedStockCodes.add(v.getStockCode());
            }
        }

        return VolatilityConverter.toReportGenerationResult(targets.size(), failedStockCodes, tradeDate);
    }

    /**
     * 리포트 날짜는 저장된 거래일을 따라야 한다. 서버 날짜를 보내면 콜백이
     * (stockCode, 그 날짜)로 행을 찾지 못해 리포트는 만들어졌는데 reportUrl이 비는 상태가 된다.
     * 연결할 행이 없으면 애초에 생성하지 않는다. GPT 비용만 나가고 쓰이지 못한다.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void runSingleReportGeneration(SingleReportRequest request) {
        Volatility target = volatilityQueryService.getLatestByStockCode(request.getStockCode());
        String reportDate = target.getTradeDate().format(REPORT_DATE_FORMATTER);

        invokeReportJob(reportDate, target.getStockCode(), request.getStockName(), request.getGptModel());
    }

    private void invokeReportJob(String reportDate, String stockCode, String stockName, String gptModel) {
        String jobId = "VOLATILITY_" + reportDate + "_" + stockCode;
        reportLambdaClient.invokeCreateReport(ReportLambdaRequestDto.builder()
                .jobId(jobId)
                .stockCode(stockCode)
                .stockName(stockName)
                .reportDate(reportDate)
                .triggerType("VOLATILITY")
                .gptModel(normalizeGptModel(gptModel))
                .build());
    }

    /**
     * Lambda는 gptModel 키가 있으면 비어 있지 않은 문자열이어야 한다고 검증한다.
     * 빈 문자열은 @JsonInclude(NON_NULL)에 걸리지 않고 그대로 나가 Lambda 실행 오류가 되므로,
     * 공백이면 null로 바꿔 키 자체가 빠지게 한다. 그러면 Lambda가 기본 모델을 쓴다.
     */
    private String normalizeGptModel(String gptModel) {
        if (gptModel == null || gptModel.isBlank()) {
            return null;
        }
        return gptModel.trim();
    }

    /**
     * 트랜잭션을 열지 않는다. 순서가 뒤집히면 안 되기 때문이다.
     *
     * 한 트랜잭션으로 묶으면 reportUrl이 아직 커밋되지 않은 상태에서 미도착 수를 세게 되고,
     * 도착 확인은 REQUIRES_NEW라 먼저 커밋된다. 바깥이 롤백되면 URL은 없는데 확인만 성공으로 남는다.
     *
     * 저장을 커맨드 서비스의 트랜잭션으로 먼저 끝내고, 커밋된 결과를 조회해 판단한다.
     * 도착 확인 갱신이 실패해도 스위퍼가 뒤에서 복구한다.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void handleReportCallback(ReportCallback request) {
        LocalDate tradeDate;
        try {
            tradeDate = LocalDate.parse(request.getReportDate(), REPORT_DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            throw VolatilityHandler.reportCallbackInvalidRequest();
        }
        volatilityCommandService.updateReportUrl(request.getStockCode(), tradeDate, request.getReportUrl());

        // 이번 콜백으로 마지막 하나가 채워졌으면 도착 확인을 끝낸다.
        long missing = volatilityQueryService.countMissingReport(tradeDate);
        if (missing > 0) {
            log.info("리포트 도착 대기 중입니다. tradeDate={} 미도착={}건", tradeDate, missing);
            return;
        }
        batchExecutionLogService.completeVerification(tradeDate, VERIFY_ALL_REPORTS_ARRIVED);
    }

    @Transactional(readOnly = true)
    public VolatilityListResponse getAllVolatilityByDate(LocalDate date) {
        return VolatilityConverter.toVolatilityListResponse(volatilityQueryService.getByTradeDate(date));
    }

    @Transactional(readOnly = true)
    public VolatilityListResponse getAllVolatilityByCode(String stockCode) {
        return VolatilityConverter.toVolatilityListResponse(volatilityQueryService.getAllVolatilityByCode(stockCode));
    }
}
