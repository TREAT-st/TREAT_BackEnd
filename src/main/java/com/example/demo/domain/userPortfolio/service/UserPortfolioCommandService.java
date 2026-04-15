package com.example.demo.domain.userPortfolio.service;

import com.example.demo.domain.userPortfolio.entity.UserPortfolio;

public interface UserPortfolioCommandService {
    UserPortfolio createPortfolio(UserPortfolio userPortfolio);
    Long deletePortfolio(Long portfolioId);
}
