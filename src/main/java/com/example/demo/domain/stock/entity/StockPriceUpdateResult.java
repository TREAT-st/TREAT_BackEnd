package com.example.demo.domain.stock.entity;

import java.util.List;

/**
 * 시세 갱신 결과.
 * 반영 건수만 돌려주면 "몇 건이 왜 빠졌는지"를 알 수 없어, 반영하지 못한 종목 코드를 함께 남긴다.
 * 같은 실행에서 목록 동기화가 끝난 뒤라 DB에 없거나 비활성인 종목이 나오는 것은
 * 단순 누락이 아니라 동기화 정합성 이상 신호다.
 */
public record StockPriceUpdateResult(
        int updatedCount,
        List<String> skippedStockCodes
) {
}
