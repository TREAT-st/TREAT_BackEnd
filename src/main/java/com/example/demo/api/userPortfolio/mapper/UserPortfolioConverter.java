package com.example.demo.api.userPortfolio.mapper;

import com.example.demo.api.userPortfolio.dto.UserPortfolioResponseDto.UserPortfolioResponse;
import com.example.demo.domain.user.entity.User;
import com.example.demo.domain.userPortfolio.entity.UserPortfolio;

public class UserPortfolioConverter {

    public static UserPortfolio toUserPortfolio(User user) {
        return UserPortfolio.builder()
                .user(user)
                .build();
    }

    public static UserPortfolioResponse toUserPortfolioResponse(UserPortfolio portfolio) {
        return UserPortfolioResponse.builder()
                .portfolioId(portfolio.getId())
                .userId(portfolio.getUser().getId())
                .totalPoint(portfolio.getTotalPoint())
                .totalPrediction(portfolio.getTotalPrediction())
                .successCount(portfolio.getSuccessCount())
                .failCount(portfolio.getFailCount())
                .virtualProfitKrw(portfolio.getVirtualProfitKrw())
                .virtualProfitPercent(portfolio.getVirtualProfitPercent())
                .build();
    }
}
