package com.example.demo.domain.volatility.repository;

import com.example.demo.domain.volatility.entity.Volatility;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface VolatilityRepository extends JpaRepository<Volatility, Long> {
    List<Volatility> findAllByStockCodeOrderByTradeDateDesc(String stockCode);

    List<Volatility> findAllByTradeDateOrderByIdAsc(LocalDate tradeDate);

    /** (stock_code, trade_date) 유니크 제약이 있으므로 최대 1건이다. */
    Optional<Volatility> findByStockCodeAndTradeDate(String stockCode, LocalDate tradeDate);

    /** 탐지 기록이 있는 가장 최근 거래일을 찾기 위한 조회. 리포트 생성 대상을 정할 때 쓴다. */
    Optional<Volatility> findFirstByOrderByTradeDateDesc();
}
