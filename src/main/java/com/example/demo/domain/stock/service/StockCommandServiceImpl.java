package com.example.demo.domain.stock.service;

import com.example.demo.domain.stock.entity.Stock;
import com.example.demo.domain.stock.exception.StockHandler;
import com.example.demo.domain.stock.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@Transactional
@RequiredArgsConstructor
public class StockCommandServiceImpl implements StockCommandService {

    private final StockRepository stockRepository;

    @Override
    public Stock updateStockPrice(
            String stockCode,
            BigDecimal openPrice,
            BigDecimal closePrice,
            LocalDate inquiryDate) {
        Stock stock = stockRepository.findByStockCode(stockCode)
                .orElseThrow(() -> StockHandler.NOT_FOUND);
        stock.updatePrice(openPrice, closePrice, inquiryDate);

        return stock;
    }
}
