package com.example.demo.domain.userPortfolio.service;

import com.example.demo.domain.userPortfolio.entity.UserPortfolio;
import com.example.demo.domain.userPortfolio.repository.UserPortfolioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class UserPortfolioCommandServiceImpl implements UserPortfolioCommandService {
    private final UserPortfolioRepository userPortfolioRepository;

    // TODO: builder로 처리하기
    @Override
    public UserPortfolio createPortfolio(UserPortfolio userPortfolio) {
        if (userPortfolioRepository.existsUserPortfolioByUserId(userPortfolio.getUser().getId())) {
            throw new IllegalStateException("Portfolio already exists for this user");
        }
        return userPortfolioRepository.save(userPortfolio);
    }

    @Override
    public Long deletePortfolio(Long portfolioId) {
        UserPortfolio portfolio = userPortfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new IllegalArgumentException("Portfolio not found: " + portfolioId));
        userPortfolioRepository.delete(portfolio);
        return portfolio.getId();
    }
}
