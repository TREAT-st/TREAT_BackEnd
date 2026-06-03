package com.example.demo.domain.stock.service;

import com.example.demo.api.stock.dto.StockSyncResultDto;
import com.example.demo.domain.stock.entity.Stock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface StockCommandService {
    Stock updateStockPrice(String stockCode, BigDecimal openPrice, BigDecimal closePrice, LocalDate inquiryDate);
    StockSyncResultDto syncStocks(Map<String, String> excelStocks);
    void saveAllStocks(List<Stock> stocks);
    void updateStockNames(Map<String, String> codeToName);
    void deleteByStockCodes(Set<String> stockCodes);
}
