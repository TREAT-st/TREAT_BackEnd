package com.example.demo.domain.userPortfolio.service;

import com.example.demo.domain.userPortfolio.entity.UserPortfolio;
import com.example.demo.domain.userPortfolio.repository.UserPortfolioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserPortfolioQueryServiceImpl implements UserPortfolioQueryService {
    private final UserPortfolioRepository userPortfolioRepository;

    @Override
    public UserPortfolio getUserPortfolioByUserId(Long userId) {
        return userPortfolioRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Portfolio not found for user: " + userId));
    }
}
