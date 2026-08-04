package com.example.demo.domain.volatility.service;

import com.example.demo.domain.volatility.entity.VolatilitySignal;

import java.time.LocalDate;
import java.util.List;

public interface VolatilityCommandService {
    void saveTopVolatilityStocks(List<VolatilitySignal> topSignals);
    void updateReportUrl(String stockCode, LocalDate reportDate, String reportUrl);
}
