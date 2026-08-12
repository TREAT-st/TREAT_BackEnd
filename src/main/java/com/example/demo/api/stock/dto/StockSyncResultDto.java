package com.example.demo.api.stock.dto;

/**
 * 코스피200 동기화 결과.
 * 편출 종목은 삭제하지 않고 비활성 처리하므로 deleted가 아니라 deactivated다.
 */
public record StockSyncResultDto(
        int addedCount,
        int updatedCount,
        int deactivatedCount,
        int reactivatedCount
) {
}
