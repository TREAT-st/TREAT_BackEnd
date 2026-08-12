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

    List<Stock> findAllByStockCodeIn(Collection<String> stockCodes);

    /** 편입 여부로 필터링. 전체를 원하면 findAll(pageable)을 쓴다. */
    Page<Stock> findAllByIsActive(Boolean isActive, Pageable pageable);

    List<Stock> findAllByIsActive(Boolean isActive);
}
