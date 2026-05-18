package com.example.demo.domain.stock.repository;

import com.example.demo.domain.stock.entity.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface StockRepository extends JpaRepository<Stock, Long> {
    boolean existsByStockCode(String stockCode);
    Optional<Stock> findByStockCode(String stockCode);
}
