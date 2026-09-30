package com.example.demo.domain.stock.repository;

import com.example.demo.domain.stock.entity.Stock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface StockRepository extends JpaRepository<Stock, Long> {
    Optional<Stock> findByStockCode(String stockCode);
    List<Stock> findAllByStockCodeInAndIsActiveTrue(Collection<String> stockCodes);
    Page<Stock> findAllByIsActive(Boolean isActive, Pageable pageable);

    @Query("select max(s.tradeDate) from Stock s")
    Optional<LocalDate> findLatestTradeDate();

    @Modifying
    @Query("update Stock s set s.likeCount = coalesce(s.likeCount, 0) + 1 where s.stockCode = :stockCode")
    int increaseLikeCount(@Param("stockCode") String stockCode);

    @Modifying
    @Query("update Stock s set s.likeCount = coalesce(s.likeCount, 0) - 1 "
            + "where s.stockCode = :stockCode and coalesce(s.likeCount, 0) > 0")
    int decreaseLikeCount(@Param("stockCode") String stockCode);

    @Modifying
    @Query("update Stock s set s.likeCount = "
            + "(select count(f) from FavoriteStock f where f.stockCode = s.stockCode)")
    int recalculateLikeCounts();
}
