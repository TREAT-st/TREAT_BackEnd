package com.example.demo.api.volatility.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
        private List<DetectedStock> stocks;
        private int detectedCount;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReportGenerationResult {
        /** SUCCESS(전부 성공) / PARTIAL_SUCCESS(일부 실패) / FAILURE(전부 실패) */
        private String status;
        private int requestedCount;
        private int successCount;
        private int failedCount;
        /** 요청에 실패해 리포트가 생성되지 않는 종목 코드. */
        private List<String> failedStockCodes;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VolatilityInfo {
        private String stockCode;
        private String stockName;
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
