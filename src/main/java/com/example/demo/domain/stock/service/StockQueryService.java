package com.example.demo.domain.stock.service;

import com.example.demo.domain.stock.entity.Stock;

import java.util.Set;

public interface StockQueryService {
    Stock getStockByCode(String stockCode);
    Set<String> getAllStockCodes();
}
