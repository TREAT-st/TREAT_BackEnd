package com.example.demo.domain.userPortfolio.service;

import com.example.demo.domain.user.entity.User;
import com.example.demo.domain.userPortfolio.entity.UserPortfolio;
import com.example.demo.domain.userPortfolio.exception.UserPortfolioHandler;
import com.example.demo.domain.userPortfolio.repository.UserPortfolioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class UserPortfolioCommandServiceImpl implements UserPortfolioCommandService {
    private final UserPortfolioRepository userPortfolioRepository;

    @Override
    public void createPortfolio(User user) {
        UserPortfolio userPortfolio = UserPortfolio.builder()
                .user(user)
                .build();
        userPortfolioRepository.save(userPortfolio);
    }

    @Override
    public Long deletePortfolio(Long portfolioId) {
        UserPortfolio portfolio = userPortfolioRepository.findById(portfolioId)
                .orElseThrow(() -> UserPortfolioHandler.NOT_FOUND);
        userPortfolioRepository.delete(portfolio);

        return portfolio.getId();
    }
}
