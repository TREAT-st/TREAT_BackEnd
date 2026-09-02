package com.example.demo.domain.stock.entity;

import java.util.List;

/** 목록 동기화 결과. 세 갈래 판정이 각각 몇 건이었는지와, 판정을 보류한 종목. */
public record StockSyncResult(
        int addedCount,
        int updatedCount,
        int deactivatedCount,
        int reactivatedCount,
        /** DB에는 활성으로 있는데 이번 응답에 안 와서, 이탈인지 알 수 없어 상태를 그대로 둔 종목. */
        List<String> unresolvedStockCodes
) {
}
