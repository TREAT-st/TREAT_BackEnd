package com.example.demo.domain.stock.repository;

import com.example.demo.domain.stock.entity.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface StockRepository extends JpaRepository<Stock, Long> {
    Optional<Stock> findByStockCode(String stockCode);

    @Query("SELECT s.stockCode FROM Stock s")
    List<String> findAllStockCodes();

    List<Stock> findAllByStockCodeIn(Collection<String> stockCodes);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Stock s WHERE s.stockCode IN :codes")
    void deleteAllByStockCodeIn(@Param("codes") Collection<String> codes);
}
