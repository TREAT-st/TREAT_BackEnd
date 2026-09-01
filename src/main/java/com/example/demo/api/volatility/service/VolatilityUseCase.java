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
import com.example.demo.domain.volatility.service.VolatilityQueryService;
import com.example.demo.domain.volatility.entity.VolatilitySignal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import static com.example.demo.api.volatility.dto.VolatilityRequestDto.ReportCallback;
import static com.example.demo.api.volatility.dto.VolatilityRequestDto.ReportGenerationRequest;
import static com.example.demo.api.volatility.dto.VolatilityRequestDto.SingleReportRequest;
import static com.example.demo.common.consts.StaticVariable.SEOUL_ZONE;

@Slf4j
@UseCase
@Transactional
@RequiredArgsConstructor
public class VolatilityUseCase {
    private final VolatilityQueryService volatilityQueryService;
    private final VolatilityDetectionService volatilityDetectionService;
    private final VolatilityCommandService volatilityCommandService;
    private final ReportLambdaClient reportLambdaClient;

    private static final DateTimeFormatter REPORT_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

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
        VolatilityDetectionResult detection = volatilityDetectionService.detect(DETECTION_REQUEST_SIZE);

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

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ReportGenerationResult runReportGeneration(ReportGenerationRequest request) {
        List<Volatility> targets = volatilityQueryService.getLatestVolatility();
        if (targets.isEmpty()) {
            throw VolatilityHandler.volatilityNotDetectedToday();
        }

        // 리포트 날짜는 탐지에 쓰인 거래일을 그대로 따른다. 서버 날짜를 쓰면 휴장일이나
        // 자정 경계에서 콜백이 조회할 행과 어긋난다.
        LocalDate tradeDate = targets.get(0).getTradeDate();
        LocalDate today = LocalDate.now(SEOUL_ZONE);
        if (!tradeDate.isEqual(today)) {
            log.warn("가장 최근 탐지 결과가 오늘이 아닙니다. 해당 거래일 기준으로 생성합니다. tradeDate={}, today={}",
                    tradeDate, today);
        }

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

    public void runSingleReportGeneration(SingleReportRequest request) {
        String reportDate = LocalDate.now(SEOUL_ZONE).format(REPORT_DATE_FORMATTER);
        invokeReportJob(reportDate, request.getStockCode(), request.getStockName(), request.getGptModel());
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

    public void handleReportCallback(ReportCallback request) {
        LocalDate tradeDate;
        try {
            tradeDate = LocalDate.parse(request.getReportDate(), REPORT_DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            throw VolatilityHandler.reportCallbackInvalidRequest();
        }
        volatilityCommandService.updateReportUrl(request.getStockCode(), tradeDate, request.getReportUrl());
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
