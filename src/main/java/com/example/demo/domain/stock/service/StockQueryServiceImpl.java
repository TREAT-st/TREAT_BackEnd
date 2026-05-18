package com.example.demo.domain.stock.service;

import com.example.demo.domain.stock.entity.Stock;
import com.example.demo.domain.stock.exception.StockErrorStatus;
import com.example.demo.domain.stock.exception.StockHandler;
import com.example.demo.domain.stock.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class StockQueryServiceImpl implements StockQueryService {

    private final StockRepository stockRepository;

    @Override
    public Stock getStockByCode(String stockCode) {
        return stockRepository.findByStockCode(stockCode)
                .orElseThrow(() -> StockHandler.NOT_FOUND);
    }
}
