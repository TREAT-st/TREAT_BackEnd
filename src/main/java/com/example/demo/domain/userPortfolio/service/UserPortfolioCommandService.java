package com.example.demo.domain.userPortfolio.service;

import com.example.demo.domain.user.entity.User;

public interface UserPortfolioCommandService {
    void createPortfolio(User user);
    Long deletePortfolio(Long portfolioId);
    void recordNewPrediction(Long userId);
    void recordGradingResult(Long userId, boolean isCorrect, long earnedPoints);
    void ensurePortfolioExists(User user);
}
