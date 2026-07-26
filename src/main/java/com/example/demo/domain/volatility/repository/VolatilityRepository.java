package com.example.demo.domain.volatility.repository;

import com.example.demo.domain.volatility.entity.Volatility;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface VolatilityRepository extends JpaRepository<Volatility, Long> {
    List<Volatility> findAllByStockCodeOrderByCreatedDateDesc(String stockCode);
    List<Volatility> findAllByCreatedDateGreaterThanEqualAndCreatedDateLessThanOrderByCreatedDateDesc(LocalDateTime start, LocalDateTime end);
}
