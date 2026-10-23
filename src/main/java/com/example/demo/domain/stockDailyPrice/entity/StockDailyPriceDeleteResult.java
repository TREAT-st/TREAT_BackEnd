package com.example.demo.domain.stockDailyPrice.entity;

/**
 * 일봉 기간 삭제 결과.
 *
 * @param deletedRows     지운 일봉 행 수
 * @param deletedSyncLogs 함께 지운 일일 적재 이력 수. 한 종목 삭제는 이력을 지우지 않아 0이다
 */
public record StockDailyPriceDeleteResult(int deletedRows, int deletedSyncLogs) {
}
