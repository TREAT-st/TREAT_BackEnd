package com.example.demo.api.volatility.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class VolatilityRequestDto {

    @Getter
    @NoArgsConstructor
    public static class ReportGenerationRequest {
        private String gptModel;
    }

    @Getter
    @NoArgsConstructor
    public static class ReportCallback {
        private String stockCode;
        private String reportDate; // "yyyyMMdd"
        private String reportUrl;
    }

    @Getter
    @NoArgsConstructor
    public static class SingleReportRequest {
        @NotBlank
        private String stockCode;
        @NotBlank
        private String stockName;
        private String gptModel;
    }
}
