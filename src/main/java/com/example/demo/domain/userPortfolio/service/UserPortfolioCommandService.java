package com.example.demo.domain.userPortfolio.service;

import com.example.demo.domain.user.entity.User;

public interface UserPortfolioCommandService {
    void createPortfolio(User user);
    Long deletePortfolio(Long portfolioId);
}
