package com.example.demo.domain.volatility.service;

import com.example.demo.domain.volatility.entity.VolatilityDetectionResult;
import com.example.demo.domain.volatility.entity.VolatilitySignal;

import java.util.List;

public interface VolatilityDetectionService {
    VolatilityDetectionResult detect(int topN);
    List<VolatilitySignal> selectTop(List<VolatilitySignal> signals, int universeSize, int topN);
}
