package com.example.demo.domain.volatility.service;

import com.example.demo.domain.volatility.entity.Volatility;

import java.time.LocalDate;
import java.util.List;

public interface VolatilityQueryService {
    List<Volatility> getByTradeDate(LocalDate tradeDate);
    List<Volatility> getAllVolatilityByCode(String stockCode);
    List<Volatility> getLatestVolatility();
}
