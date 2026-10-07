package com.example.demo.domain.stock.entity;

import java.math.BigDecimal;

/**
 * 한 거래일의 종목 시세. 시가총액은 못 받을 수 있어 null을 허용한다.
 * 시가총액 누락만으로 종목을 목록에서 빼지 않는다 — 시세 실패를 편출로 오판하지 않는 것과 같은 이유다.
 */
public record StockPriceSnapshot(
        String stockCode,
        BigDecimal openPrice,
        BigDecimal closePrice,
        Long marketCapitalization
) {
}
