package com.example.demo.api.userPortfolio.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class UserPortfolioRequestDto {

    @Getter
    @NoArgsConstructor
    public static class UserPortfolioRequest {
        @NotNull
        private Long totalPoint;
        @NotNull
        private Long totalPrediction;
        @NotNull
        private Long successCount;
        @NotNull
        private Long failCount;
        @NotNull
        private Double virtualProfitKrw;
        @NotNull
        private Double virtualProfitPercent;
    }
}
