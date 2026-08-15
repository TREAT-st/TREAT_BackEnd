package com.example.demo.domain.stock.service;

import com.example.demo.domain.stock.entity.Stock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface StockQueryService {
    Stock getStockByCode(String stockCode);
    Page<Stock> getAllStocks(Boolean isActive, Pageable pageable);
}
