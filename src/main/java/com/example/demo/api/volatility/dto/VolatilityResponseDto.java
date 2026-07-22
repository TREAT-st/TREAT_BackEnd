package com.example.demo.api.volatility.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

public class VolatilityResponseDto {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VolatilityInfo {
        private String volatilityCode;
        private String volatilityName;
        private String reportUrl;
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
