package com.example.demo.domain.stock.entity;

import java.math.BigDecimal;

public record StockPriceSnapshot(
        String stockCode,
        BigDecimal openPrice,
        BigDecimal closePrice
) {
}
