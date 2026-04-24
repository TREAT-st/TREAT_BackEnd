package com.example.demo.domain.userPortfolio.service;

import com.example.demo.api.userPortfolio.dto.UserPortfolioRequestDto.*;
import com.example.demo.domain.user.entity.User;
import com.example.demo.domain.user.exception.UserHandler;
import com.example.demo.domain.user.repository.UserRepository;
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
    private final UserRepository userRepository;

    @Override
    public void createPortfolio(Long userId) {
        if (userPortfolioRepository.existsUserPortfolioByUserId(userId)) {
            throw UserPortfolioHandler.ALREADY_EXISTS;
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> UserHandler.NOT_FOUND);
        UserPortfolio userPortfolio = UserPortfolio.builder()
                .user(user)
                .build();
        userPortfolioRepository.save(userPortfolio);
    }

    @Override
    public void updateUserPortfolio(Long userId, Long totalPoint, Long totalPrediction,
                                    Long successCount, Long failCount,
                                    Double virtualProfitKrw, Double virtualProfitPercent) {
        UserPortfolio portfolio = userPortfolioRepository.findByUserId(userId)
                .orElseThrow(() -> UserPortfolioHandler.NOT_FOUND);

        portfolio.updateUserPortfolio(totalPoint, totalPrediction, successCount, failCount,
                virtualProfitKrw, virtualProfitPercent);
    }

    @Override
    public Long deletePortfolio(Long portfolioId) {
        UserPortfolio portfolio = userPortfolioRepository.findById(portfolioId)
                .orElseThrow(() -> UserPortfolioHandler.NOT_FOUND);
        userPortfolioRepository.delete(portfolio);

        return portfolio.getId();
    }
}
