package com.example.demo.domain.stock.repository;

import com.example.demo.domain.stock.entity.Stock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface StockRepository extends JpaRepository<Stock, Long> {
    Optional<Stock> findByStockCode(String stockCode);
    List<Stock> findAllByStockCodeInAndIsActiveTrue(Collection<String> stockCodes);
    Page<Stock> findAllByIsActive(Boolean isActive, Pageable pageable);

    /**
     * 시세가 반영된 가장 최근 거래일. 아직 아무 시세도 없으면 비어 있다.
     * Stock은 종목별 최신 스냅샷만 보관하므로 max(tradeDate)가 마지막 동기화 기준일이다.
     */
    @Query("select max(s.tradeDate) from Stock s")
    Optional<LocalDate> findLatestTradeDate();
}
