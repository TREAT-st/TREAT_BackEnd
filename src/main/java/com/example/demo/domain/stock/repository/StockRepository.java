package com.example.demo.domain.stock.repository;

import com.example.demo.domain.stock.entity.Stock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface StockRepository extends JpaRepository<Stock, Long> {
    Optional<Stock> findByStockCode(String stockCode);
    List<Stock> findAllByStockCodeInAndIsActiveTrue(Collection<String> stockCodes);
    Page<Stock> findAllByIsActive(Boolean isActive, Pageable pageable);
}
