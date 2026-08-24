package com.example.demo.domain.userPortfolio.service;

import com.example.demo.domain.userPortfolio.entity.UserPortfolio;

public interface UserPortfolioQueryService {
    UserPortfolio getUserPortfolioByUserId(Long userId);
}
