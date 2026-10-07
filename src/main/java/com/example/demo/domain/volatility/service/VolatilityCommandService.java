package com.example.demo.domain.volatility.service;

import com.example.demo.domain.volatility.entity.VolatilitySignal;

import java.time.LocalDate;
import java.util.List;

public interface VolatilityCommandService {
    void saveTopVolatilityStocks(List<VolatilitySignal> topSignals, LocalDate tradeDate);
    void updateReportUrl(String stockCode, LocalDate tradeDate, String reportUrl);

    /** 비어 있을 때만 채운다. S3 보정 전용. @return 갱신된 행 수 */
    int fillReportUrlIfAbsent(String stockCode, LocalDate tradeDate, String reportUrl);
}
