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
