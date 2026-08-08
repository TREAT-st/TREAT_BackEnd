package com.example.demo.api.volatility.service;

import com.example.demo.api.report.client.ReportLambdaClient;
import com.example.demo.api.report.dto.ReportLambdaRequestDto;
import com.example.demo.api.volatility.dto.VolatilityResponseDto.DetectionResult;
import com.example.demo.api.volatility.dto.VolatilityResponseDto.ReportGenerationResult;
import com.example.demo.api.volatility.dto.VolatilityResponseDto.VolatilityListResponse;
import com.example.demo.api.volatility.mapper.VolatilityConverter;
import com.example.demo.common.annotation.UseCase;
import com.example.demo.domain.volatility.entity.Volatility;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import static com.example.demo.api.volatility.dto.VolatilityRequestDto.ReportCallback;
import static com.example.demo.api.volatility.dto.VolatilityRequestDto.ReportGenerationRequest;
import static com.example.demo.api.volatility.dto.VolatilityRequestDto.SingleReportRequest;

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

    public DetectionResult runDetection() {
        List<VolatilitySignal> signals = volatilityDetectionService.detect(100);
        List<VolatilitySignal> top10 = volatilityDetectionService.selectTop(signals, 100, 10);
        volatilityCommandService.saveTopVolatilityStocks(top10);
        return VolatilityConverter.toDetectionResult(top10);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ReportGenerationResult runReportGeneration(ReportGenerationRequest request) {
        List<Volatility> todayVolatility = volatilityQueryService.getTodayVolatility();
        if (todayVolatility.isEmpty()) {
            throw VolatilityHandler.volatilityNotDetectedToday();
        }

        String reportDate = LocalDate.now().format(REPORT_DATE_FORMATTER);

        // 한 종목이 실패해도 나머지 종목의 리포트 생성은 계속 요청한다.
        List<String> failedStockCodes = new ArrayList<>();
        for (Volatility v : todayVolatility) {
            try {
                invokeReportJob(reportDate, v.getStockCode(), v.getStockName(), request.getGptModel());
            } catch (Exception e) {
                log.error("리포트 생성 요청 실패, 다음 종목으로 진행. stockCode={}", v.getStockCode(), e);
                failedStockCodes.add(v.getStockCode());
            }
        }

        return VolatilityConverter.toReportGenerationResult(todayVolatility.size(), failedStockCodes);
    }

    public void runSingleReportGeneration(SingleReportRequest request) {
        String reportDate = LocalDate.now().format(REPORT_DATE_FORMATTER);
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
                .gptModel(gptModel)
                .build());
    }

    public void handleReportCallback(ReportCallback request) {
        LocalDate reportDate;
        try {
            reportDate = LocalDate.parse(request.getReportDate(), REPORT_DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            throw VolatilityHandler.reportCallbackInvalidRequest();
        }
        volatilityCommandService.updateReportUrl(request.getStockCode(), reportDate, request.getReportUrl());
    }

    @Transactional(readOnly = true)
    public VolatilityListResponse getAllVolatilityByDate(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();
        return VolatilityConverter.toVolatilityListResponse(volatilityQueryService.getAllVolatilityByDate(start, end));
    }

    @Transactional(readOnly = true)
    public VolatilityListResponse getAllVolatilityByCode(String stockCode) {
        return VolatilityConverter.toVolatilityListResponse(volatilityQueryService.getAllVolatilityByCode(stockCode));
    }
}
