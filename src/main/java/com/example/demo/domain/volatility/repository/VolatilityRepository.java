package com.example.demo.domain.volatility.repository;

import com.example.demo.domain.volatility.entity.Volatility;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface VolatilityRepository extends JpaRepository<Volatility, Long> {
    List<Volatility> findAllByStockCodeOrderByTradeDateDesc(String stockCode);

    Optional<Volatility> findFirstByStockCodeOrderByTradeDateDesc(String stockCode);

    List<Volatility> findAllByTradeDateOrderByIdAsc(LocalDate tradeDate);

    /**
     * 저장 경로 전용. 같은 거래일에 대한 동시 탐지가 서로의 스냅샷을 못 보고
     * 삭제·삽입을 교차시키면 최종 집합이 어느 쪽 결과와도 달라진다.
     * 해당 거래일의 행을 잠가 회차 단위로 직렬화한다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM Volatility v WHERE v.tradeDate = :tradeDate ORDER BY v.id ASC")
    List<Volatility> findAllByTradeDateForUpdate(@Param("tradeDate") LocalDate tradeDate);

    /** (stock_code, trade_date) 유니크 제약이 있으므로 최대 1건이다. */
    Optional<Volatility> findByStockCodeAndTradeDate(String stockCode, LocalDate tradeDate);

    /** 탐지 기록이 있는 가장 최근 거래일을 찾기 위한 조회. 리포트 생성 대상을 정할 때 쓴다. */
    Optional<Volatility> findFirstByOrderByTradeDateDesc();
}
