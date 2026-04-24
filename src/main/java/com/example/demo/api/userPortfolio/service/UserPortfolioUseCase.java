package com.example.demo.api.userPortfolio.service;

import com.example.demo.api.userPortfolio.dto.UserPortfolioRequestDto.*;
import com.example.demo.common.annotation.UseCase;
import com.example.demo.domain.userPortfolio.entity.UserPortfolio;
import com.example.demo.domain.userPortfolio.service.UserPortfolioCommandService;
import com.example.demo.domain.userPortfolio.service.UserPortfolioQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@Transactional
@RequiredArgsConstructor
public class UserPortfolioUseCase {
    private final UserPortfolioCommandService userPortfolioCommandService;
    private final UserPortfolioQueryService userPortfolioQueryService;

    public UserPortfolio updateUserPortfolio(Long userId, UserPortfolioRequest request) {
        userPortfolioCommandService.updateUserPortfolio(
                userId,
                request.getTotalPoint(),
                request.getTotalPrediction(),
                request.getSuccessCount(),
                request.getFailCount(),
                request.getVirtualProfitKrw(),
                request.getVirtualProfitPercent()
        );

        return userPortfolioQueryService.getUserPortfolioByUserId(userId);
    }

    public Long deleteUserPortfolio(Long userId) {
        Long portfolioId = userPortfolioQueryService.getUserPortfolioByUserId(userId).getId();
        return userPortfolioCommandService.deletePortfolio(portfolioId);
    }
}
