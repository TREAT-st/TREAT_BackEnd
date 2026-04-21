package com.example.demo.domain.userPortfolio.service;

import com.example.demo.api.userPortfolio.dto.UserPortfolioRequestDto.UpdateUserPortfolioRequest;

public interface UserPortfolioCommandService {
    void createPortfolio(Long userId);
    void updateUserPortfolio(Long userId, UpdateUserPortfolioRequest request);
    Long deletePortfolio(Long portfolioId);
}
