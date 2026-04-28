package com.example.demo.domain.userPortfolio.service;

import com.example.demo.domain.user.entity.User;

public interface UserPortfolioCommandService {
    void createPortfolio(User user);
    void updateUserPortfolio(Long userId, Long totalPoint, Long totalPrediction,
                             Long successCount, Long failCount,
                             Double virtualProfitKrw, Double virtualProfitPercent);
    Long deletePortfolio(Long portfolioId);
}
