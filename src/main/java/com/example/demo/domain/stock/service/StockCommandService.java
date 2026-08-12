package com.example.demo.domain.stock.service;

import com.example.demo.api.stock.dto.StockSyncResultDto;
import com.example.demo.domain.stock.entity.Stock;
import com.example.demo.domain.stock.entity.StockPriceSnapshot;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface StockCommandService {
    StockSyncResultDto syncStocks(Map<String, String> sourceStocks);

    /** 여러 종목의 시세를 한 번에 갱신하고 실제 반영된 건수를 돌려준다. */
    int updateStockPrices(List<StockPriceSnapshot> snapshots, LocalDate inquiryDate);
}
