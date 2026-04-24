package com.example.demo.api.userPortfolio.mapper;

import com.example.demo.api.userPortfolio.dto.UserPortfolioRequestDto.*;
import com.example.demo.api.userPortfolio.dto.UserPortfolioResponseDto.UserPortfolioResponse;
import com.example.demo.domain.user.entity.User;
import com.example.demo.domain.userPortfolio.entity.UserPortfolio;

public class UserPortfolioConverter {

    public static UserPortfolio toUserPortfolio(User user, UserPortfolioRequest userPortfolioRequest) {
        return UserPortfolio.builder()
                .user(user)
                .totalPoint(userPortfolioRequest.getTotalPoint())
                .totalPrediction(userPortfolioRequest.getTotalPrediction())
                .successCount(userPortfolioRequest.getSuccessCount())
                .failCount(userPortfolioRequest.getFailCount())
                .virtualProfitKrw(userPortfolioRequest.getVirtualProfitKrw())
                .virtualProfitPercent(userPortfolioRequest.getVirtualProfitPercent())
                .build();
    }

    public static UserPortfolioResponse toUserPortfolioResponse(UserPortfolio userPortfolio) {
        return UserPortfolioResponse.builder()
                .portfolioId(userPortfolio.getId())
                .userId(userPortfolio.getUser().getId())
                .totalPoint(userPortfolio.getTotalPoint())
                .totalPrediction(userPortfolio.getTotalPrediction())
                .successCount(userPortfolio.getSuccessCount())
                .failCount(userPortfolio.getFailCount())
                .virtualProfitKrw(userPortfolio.getVirtualProfitKrw())
                .virtualProfitPercent(userPortfolio.getVirtualProfitPercent())
                .build();
    }
}
