package com.example.demo.api.volatility.mapper;

import com.example.demo.api.volatility.dto.VolatilityResponseDto.DetectedStock;
import com.example.demo.api.volatility.dto.VolatilityResponseDto.DetectionResult;
import com.example.demo.api.volatility.dto.VolatilityResponseDto.ReportGenerationResult;
import com.example.demo.api.volatility.dto.VolatilityResponseDto.VolatilityInfo;
import com.example.demo.api.volatility.dto.VolatilityResponseDto.VolatilityListResponse;
import com.example.demo.domain.volatility.entity.Volatility;
import com.example.demo.domain.volatility.entity.VolatilitySignal;

import java.util.List;
import java.util.stream.Collectors;

public class VolatilityConverter {

    public static DetectionResult toDetectionResult(List<VolatilitySignal> signals) {
        List<DetectedStock> stocks = signals.stream()
                .map(s -> DetectedStock.builder()
                        .stockCode(s.stockCode())
                        .stockName(s.stockName())
                        .build())
                .collect(Collectors.toList());
        return DetectionResult.builder()
                .stocks(stocks)
                .detectedCount(stocks.size())
                .build();
    }

    public static ReportGenerationResult toReportGenerationResult(int requestedCount, boolean success) {
        return ReportGenerationResult.builder()
                .status(success ? "SUCCESS" : "FAILURE")
                .requestedCount(requestedCount)
                .build();
    }

    public static VolatilityInfo toVolatilityInfo(Volatility volatility) {
        return VolatilityInfo.builder()
                .stockCode(volatility.getStockCode())
                .stockName(volatility.getStockName())
                .reportUrl(volatility.getReportUrl())
                .createdDate(volatility.getCreatedDate())
                .build();
    }

    public static VolatilityListResponse toVolatilityListResponse(List<Volatility> volatilises) {
        List<VolatilityInfo> content = volatilises.stream()
                .map(VolatilityConverter::toVolatilityInfo)
                .collect(Collectors.toList());

        return VolatilityListResponse.builder()
                .content(content)
                .totalCount(content.size())
                .build();
    }
}
