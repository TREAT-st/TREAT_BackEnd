package com.example.demo.domain.volatility.service;

import com.example.demo.domain.volatility.entity.Volatility;

import java.util.List;

public interface VolatilityQueryService {
    List<Volatility> getAllVolatility();
    Volatility getVolatilityByCode(String stockCode);
}
