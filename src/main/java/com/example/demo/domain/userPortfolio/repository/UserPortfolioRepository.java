package com.example.demo.domain.userPortfolio.repository;

import com.example.demo.domain.userPortfolio.entity.UserPortfolio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserPortfolioRepository extends JpaRepository<UserPortfolio, Long> {
    Optional<UserPortfolio> findByUserId(Long userId);
    boolean existsUserPortfolioByUserId(Long userId);
}
