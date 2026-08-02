package com.example.demo.domain.volatility.service;

import java.util.List;

public interface VolatilityCommandService {
    void saveTopVolatilityStocks(List<VolatilitySignal> topSignals);
}
