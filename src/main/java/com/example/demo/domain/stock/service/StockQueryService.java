package com.example.demo.domain.stock.service;

import com.example.demo.domain.stock.entity.Stock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Set;

public interface StockQueryService {
    Stock getStockByCode(String stockCode);
    Set<String> getAllStockCodes();
    Page<Stock> getAllStocks(Pageable pageable);
}
