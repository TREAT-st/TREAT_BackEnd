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
    /** 시세 갱신 대상. 편출된 종목은 더 이상 시세를 받지 않으므로 활성 종목만 조회한다. */
    List<Stock> findAllByStockCodeInAndIsActiveTrue(Collection<String> stockCodes);
    Page<Stock> findAllByIsActive(Boolean isActive, Pageable pageable);
    List<Stock> findAllByIsActive(Boolean isActive);
}
