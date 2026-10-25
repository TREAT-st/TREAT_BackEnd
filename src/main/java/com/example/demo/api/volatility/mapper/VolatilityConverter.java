package com.example.demo.api.volatility.mapper;

import com.example.demo.api.volatility.dto.VolatilityResponseDto.DetectedStock;
import com.example.demo.api.volatility.dto.VolatilityResponseDto.DetectionResult;
import com.example.demo.api.volatility.dto.VolatilityResponseDto.ReportGenerationResult;
import com.example.demo.api.volatility.dto.VolatilityResponseDto.VolatilityInfo;
import com.example.demo.api.volatility.dto.VolatilityResponseDto.VolatilityListResponse;
import com.example.demo.domain.volatility.entity.Volatility;
import com.example.demo.domain.volatility.entity.VolatilitySignal;

import java.time.LocalDate;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import static com.example.demo.common.consts.StaticVariable.*;

public class VolatilityConverter {

    public static DetectionResult toDetectionResult(List<VolatilitySignal> signals, LocalDate tradeDate) {
        List<DetectedStock> stocks = signals.stream()
                .map(s -> DetectedStock.builder()
                        .stockCode(s.stockCode())
                        .stockName(s.stockName())
                        .build())
                .collect(Collectors.toList());
        return DetectionResult.builder()
                .tradeDate(tradeDate)
                .stocks(stocks)
                .detectedCount(stocks.size())
                .build();
    }

    public static ReportGenerationResult toReportGenerationResult(int requestedCount, List<String> failedStockCodes,
                                                                 LocalDate tradeDate) {
        int failedCount = failedStockCodes.size();
        String status;
        if (failedCount == 0) {
            status = REPORT_GENERATION_SUCCESS;
        } else if (failedCount == requestedCount) {
            status = REPORT_GENERATION_FAILURE;
        } else {
            status = REPORT_GENERATION_PARTIAL_SUCCESS;
        }

        return ReportGenerationResult.builder()
                .status(status)
                .tradeDate(tradeDate)
                .requestedCount(requestedCount)
                .successCount(requestedCount - failedCount)
                .failedCount(failedCount)
                .failedStockCodes(failedStockCodes)
                .build();
    }

    /** @param reportUrlResolver 저장된 URL을 응답용 URL로 바꾼다. 리포트 버킷이 private이라 서명이 필요하다. */
    public static VolatilityInfo toVolatilityInfo(Volatility volatility, UnaryOperator<String> reportUrlResolver) {
        return VolatilityInfo.builder()
                .stockCode(volatility.getStockCode())
                .stockName(volatility.getStockName())
                .tradeDate(volatility.getTradeDate())
                .reportUrl(reportUrlResolver.apply(volatility.getReportUrl()))
                .createdDate(volatility.getCreatedDate())
                .build();
    }

    public static VolatilityListResponse toVolatilityListResponse(List<Volatility> volatilises,
                                                                  UnaryOperator<String> reportUrlResolver) {
        List<VolatilityInfo> content = volatilises.stream()
                .map(v -> toVolatilityInfo(v, reportUrlResolver))
                .collect(Collectors.toList());

        return VolatilityListResponse.builder()
                .content(content)
                .totalCount(content.size())
                .build();
    }
}
