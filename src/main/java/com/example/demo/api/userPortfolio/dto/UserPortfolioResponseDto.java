package com.example.demo.api.userPortfolio.dto;

import lombok.Builder;
import lombok.Getter;

public class UserPortfolioResponseDto {

    @Getter
    @Builder
    public static class UserPortfolioResponse {
        private Long portfolioId;
        private Long userId;
        private Long totalPoint;
        private Long totalPrediction;
        private Long successCount;
        private Long failCount;
        private Double virtualProfitKrw;
        private Double virtualProfitPercent;
    }
}
