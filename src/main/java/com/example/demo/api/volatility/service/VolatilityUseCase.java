package com.example.demo.api.volatility.service;

import com.example.demo.api.report.client.ReportLambdaClient;
import com.example.demo.api.report.dto.ReportLambdaRequestDto;
import com.example.demo.api.volatility.dto.VolatilityResponseDto.DetectionResult;
import com.example.demo.api.volatility.dto.VolatilityResponseDto.ReportGenerationResult;
import com.example.demo.api.volatility.dto.VolatilityResponseDto.VolatilityListResponse;
import com.example.demo.api.volatility.mapper.VolatilityConverter;
import com.example.demo.common.annotation.UseCase;
import com.example.demo.common.service.RedisService;
import com.example.demo.domain.volatility.entity.Volatility;
import com.example.demo.domain.volatility.service.VolatilityCommandService;
import com.example.demo.domain.volatility.service.VolatilityDetectionService;
import com.example.demo.domain.volatility.service.VolatilityQueryService;
import com.example.demo.domain.volatility.service.VolatilitySignal;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static com.example.demo.api.volatility.dto.VolatilityRequestDto.ReportCallback;
import static com.example.demo.api.volatility.dto.VolatilityRequestDto.ReportGenerationRequest;
import static com.example.demo.api.volatility.dto.VolatilityRequestDto.SingleReportRequest;

@UseCase
@Transactional
@RequiredArgsConstructor
public class VolatilityUseCase {
    private final VolatilityQueryService volatilityQueryService;
    private final VolatilityDetectionService volatilityDetectionService;
    private final VolatilityCommandService volatilityCommandService;
    private final ReportLambdaClient reportLambdaClient;
    private final RedisService redisService;

    private static final DateTimeFormatter REPORT_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    public DetectionResult runDetection() {
        List<VolatilitySignal> signals = volatilityDetectionService.detect(100);
        List<VolatilitySignal> top10 = volatilityDetectionService.selectTop(signals, 100, 10);
        volatilityCommandService.saveTopVolatilityStocks(top10);
        return VolatilityConverter.toDetectionResult(top10);
    }

    public ReportGenerationResult runReportGeneration(ReportGenerationRequest request) {
        List<Volatility> todayVolatility = volatilityQueryService.getTodayVolatility();
        if (todayVolatility.isEmpty()) {
            throw new IllegalStateException("오늘 탐지된 변동성 종목이 없습니다. 먼저 /detect를 실행하세요.");
        }

        String reportDate = LocalDate.now().format(REPORT_DATE_FORMATTER);
        redisService.setVolatilityReportJob(reportDate, "PENDING");

        for (Volatility v : todayVolatility) {
            String jobId = "VOLATILITY_" + reportDate + "_" + v.getStockCode();
            reportLambdaClient.invokeCreateReport(ReportLambdaRequestDto.builder()
                    .jobId(jobId)
                    .stockCode(v.getStockCode())
                    .stockName(v.getStockName())
                    .reportDate(reportDate)
                    .triggerType("VOLATILITY")
                    .gptModel(request.getGptModel())
                    .build());
        }

        return VolatilityConverter.toReportGenerationResult(todayVolatility.size(), true);
    }

    public void runSingleReportGeneration(SingleReportRequest request) {
        String reportDate = LocalDate.now().format(REPORT_DATE_FORMATTER);
        String jobId = "VOLATILITY_" + reportDate + "_" + request.getStockCode();
        reportLambdaClient.invokeCreateReport(ReportLambdaRequestDto.builder()
                .jobId(jobId)
                .stockCode(request.getStockCode())
                .stockName(request.getStockName())
                .reportDate(reportDate)
                .triggerType("VOLATILITY")
                .gptModel(request.getGptModel())
                .build());
    }

    public void handleReportCallback(ReportCallback request) {
        LocalDate reportDate = LocalDate.parse(request.getReportDate(), REPORT_DATE_FORMATTER);
        volatilityCommandService.updateReportUrl(request.getStockCode(), reportDate, request.getReportUrl());
        redisService.setVolatilityReportJob(request.getReportDate(), "COMPLETED");
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
