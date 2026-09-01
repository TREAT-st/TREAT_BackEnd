package com.example.demo.api.volatility.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class VolatilityResponseDto {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DetectedStock {
        private String stockCode;
        private String stockName;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DetectionResult {
        private LocalDate tradeDate;
        private List<DetectedStock> stocks;
        private int detectedCount;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReportGenerationResult {
        private String status;
        private LocalDate tradeDate;
        private int requestedCount;
        private int successCount;
        private int failedCount;
        private List<String> failedStockCodes;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VolatilityInfo {
        private String stockCode;
        private String stockName;
        private LocalDate tradeDate;
        private String reportUrl;
        private LocalDateTime createdDate;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VolatilityListResponse {
        private List<VolatilityInfo> content;
        private int totalCount;
    }
}
