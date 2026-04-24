package com.example.demo.domain.userPortfolio.service;

public interface UserPortfolioCommandService {
    void createPortfolio(Long userId);
    void updateUserPortfolio(Long userId, Long totalPoint, Long totalPrediction,
                             Long successCount, Long failCount,
                             Double virtualProfitKrw, Double virtualProfitPercent);
    Long deletePortfolio(Long portfolioId);
}
