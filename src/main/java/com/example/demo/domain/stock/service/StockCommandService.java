package com.example.demo.domain.stock.service;

import com.example.demo.domain.stock.entity.Stock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface StockCommandService {
    Stock updateStockPrice(String stockCode, BigDecimal openPrice, BigDecimal closePrice, LocalDate inquiryDate);
    void saveAllStocks(List<Stock> stocks);
}
