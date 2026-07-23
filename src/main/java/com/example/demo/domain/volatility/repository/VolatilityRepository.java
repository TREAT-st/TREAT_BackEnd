package com.example.demo.domain.volatility.repository;

import com.example.demo.domain.volatility.entity.Volatility;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VolatilityRepository extends JpaRepository<Volatility, Long> {
    Optional<Volatility> findByStockCode(String stockCode);
    List<Volatility> findAllByOrderByCreatedDateDesc();
}
