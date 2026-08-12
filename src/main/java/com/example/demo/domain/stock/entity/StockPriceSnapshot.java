package com.example.demo.domain.stock.entity;

import java.math.BigDecimal;

/**
 * 특정 거래일의 종목 시가·종가.
 * 시세 출처(KRX Lambda 등)에 도메인이 묶이지 않도록 두는 값 객체.
 */
public record StockPriceSnapshot(
        String stockCode,
        BigDecimal openPrice,
        BigDecimal closePrice
) {
}
