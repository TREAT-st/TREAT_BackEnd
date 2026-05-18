package com.example.demo.domain.stock.service;

import com.example.demo.domain.stock.entity.Stock;

public interface StockQueryService {
    Stock getStockByCode(String stockCode);
}
