package com.example.demo.domain.stock.entity;


import java.util.List;

/** 목록 동기화와 시세 갱신을 한 트랜잭션으로 묶어 실행한 결과. */
public record StockSyncOutcome(
        StockSyncResult syncResult,
        int priceUpdatedCount,
        List<String> priceUpdateSkippedStockCodes
) {
}
