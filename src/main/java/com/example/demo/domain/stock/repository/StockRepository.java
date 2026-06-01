package com.example.demo.domain.stock.repository;

import com.example.demo.domain.stock.entity.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface StockRepository extends JpaRepository<Stock, Long> {
    Optional<Stock> findByStockCode(String stockCode);

    @Query("SELECT s.stockCode FROM Stock s")
    List<String> findAllStockCodes();
}
