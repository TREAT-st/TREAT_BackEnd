package com.example.demo.domain.stockDailyPrice.repository;

import com.example.demo.domain.stockDailyPrice.entity.DailyPriceSyncStatus;
import com.example.demo.domain.stockDailyPrice.entity.StockDailyPriceSyncLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface StockDailyPriceSyncLogRepository extends JpaRepository<StockDailyPriceSyncLog, Long> {

    Optional<StockDailyPriceSyncLog> findByTradeDate(LocalDate tradeDate);

    List<StockDailyPriceSyncLog> findAllByTradeDateBetween(LocalDate from, LocalDate to);

    /** 첫 이력의 거래일. 일일 적재가 이력이 하나라도 있는지 판단하는 데 쓴다. */
    @Query("select min(l.tradeDate) from StockDailyPriceSyncLog l")
    Optional<LocalDate> findEarliestTradeDate();

    /** 끝난 상태(SUCCESS·HOLIDAY)의 마지막 거래일. */
    @Query("select max(l.tradeDate) from StockDailyPriceSyncLog l where l.status in :statuses")
    Optional<LocalDate> findLatestTradeDateByStatusIn(@Param("statuses") Collection<DailyPriceSyncStatus> statuses);

    /** 다시 받을 날짜. 시도 횟수 상한 미만인 PARTIAL·FAILED. */
    List<StockDailyPriceSyncLog> findAllByStatusInAndAttemptLessThanOrderByTradeDateAsc(
            Collection<DailyPriceSyncStatus> statuses, int attempt);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from StockDailyPriceSyncLog l where l.tradeDate between :from and :to")
    int deleteAllByTradeDateBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
