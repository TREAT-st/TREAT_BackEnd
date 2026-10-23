package com.example.demo.domain.stockDailyPrice.repository;

import com.example.demo.domain.stockDailyPrice.entity.StockDailyPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface StockDailyPriceRepository extends JpaRepository<StockDailyPrice, Long> {

    /** 차트 조회. (stock_code, trade_date) 유니크 인덱스를 그대로 탄다. */
    List<StockDailyPrice> findAllByStockCodeAndTradeDateBetweenOrderByTradeDateAsc(
            String stockCode, LocalDate from, LocalDate to);

    /** 차트 기간의 기준점. 오늘이 아니라 그 종목에 실제로 쌓인 마지막 거래일에서 거꾸로 센다. */
    @Query("select max(p.tradeDate) from StockDailyPrice p where p.stockCode = :stockCode")
    Optional<LocalDate> findLatestTradeDate(@Param("stockCode") String stockCode);

    /** 일일 적재 이력이 아직 없을 때의 시작점. 백필 직후 첫 일일 실행이 백필 다음 날부터 이어 받는다. */
    @Query("select max(p.tradeDate) from StockDailyPrice p")
    Optional<LocalDate> findLatestTradeDateOverall();

    /** upsert용. 이미 있는 행을 한 번에 읽어 갱신과 신규를 가른다. */
    List<StockDailyPrice> findAllByStockCodeInAndTradeDateBetween(
            Collection<String> stockCodes, LocalDate from, LocalDate to);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from StockDailyPrice p where p.tradeDate between :from and :to")
    int deleteAllByTradeDateBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from StockDailyPrice p where p.stockCode = :stockCode and p.tradeDate between :from and :to")
    int deleteAllByStockCodeAndTradeDateBetween(@Param("stockCode") String stockCode,
                                                @Param("from") LocalDate from, @Param("to") LocalDate to);
}
