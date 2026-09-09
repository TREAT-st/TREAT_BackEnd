package com.example.demo.domain.userPortfolio.service;

import com.example.demo.domain.user.entity.User;
import com.example.demo.domain.userPortfolio.entity.UserPortfolio;
import com.example.demo.domain.userPortfolio.exception.UserPortfolioHandler;
import com.example.demo.domain.userPortfolio.repository.UserPortfolioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
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

    @Override
    public void recordNewPrediction(Long userId) {
        UserPortfolio portfolio = userPortfolioRepository.findByUserId(userId)
                .orElseThrow(() -> UserPortfolioHandler.NOT_FOUND);
        portfolio.addPrediction();
    }

    @Override
    public void recordGradingResult(Long userId, boolean isCorrect, long earnedPoints) {
        UserPortfolio portfolio = userPortfolioRepository.findByUserId(userId)
                .orElseThrow(() -> UserPortfolioHandler.NOT_FOUND);
        if (isCorrect) {
            portfolio.recordSuccess(earnedPoints);
        } else {
            portfolio.recordFailure();
        }
    }

    @Override
    public void ensurePortfolioExists(User user) {
        if (!userPortfolioRepository.existsUserPortfolioByUserId(user.getId())) {
            log.warn("[Portfolio] 유저 {}의 포트폴리오가 없어 지연 생성합니다.", user.getId());
            createPortfolio(user);
        }
    }
}
