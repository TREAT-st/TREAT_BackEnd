package com.example.demo.domain.volatility.service;

import com.example.demo.domain.volatility.entity.Volatility;

import java.time.LocalDate;
import java.util.List;

public interface VolatilityQueryService {
    List<Volatility> getByTradeDate(LocalDate tradeDate);
    List<Volatility> getAllVolatilityByCode(String stockCode);
    List<Volatility> getLatestVolatility();

    /** 해당 종목의 가장 최근 탐지 기록. 리포트를 연결할 행을 특정할 때 쓴다. */
    Volatility getLatestByStockCode(String stockCode);
}
