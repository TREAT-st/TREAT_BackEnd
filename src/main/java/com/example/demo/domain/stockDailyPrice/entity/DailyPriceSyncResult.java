package com.example.demo.domain.stockDailyPrice.entity;

import java.time.LocalDateTime;
import java.util.List;

/** 거래일 하나를 적재한 결과. 적재 이력에 그대로 기록된다. */
public record DailyPriceSyncResult(
        DailyPriceSyncStatus status,
        int requestedStockCount,
        int upsertedRows,
        int skippedStockCount,
        List<String> failedStockCodes,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        String message
) {
}
