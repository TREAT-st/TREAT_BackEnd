package com.example.demo.domain.stock.service;

import com.example.demo.domain.stock.entity.Stock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface StockQueryService {
    Stock getStockByCode(String stockCode);
    Page<Stock> getAllActiveStocks(Pageable pageable);
    List<Stock> getActiveStocks();
}
