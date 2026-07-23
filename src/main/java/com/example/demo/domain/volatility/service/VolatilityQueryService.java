package com.example.demo.domain.volatility.service;

import com.example.demo.domain.volatility.entity.Volatility;

import java.time.LocalDateTime;
import java.util.List;

public interface VolatilityQueryService {
    List<Volatility> getAllVolatilityByDate(LocalDateTime start, LocalDateTime end);
    List<Volatility> getAllVolatilityByCode(String stockCode);
}
