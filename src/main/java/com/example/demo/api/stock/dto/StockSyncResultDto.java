package com.example.demo.api.stock.dto;

public record StockSyncResultDto(
        int addedCount,
        int updatedCount,
        int deletedCount
) {
}
