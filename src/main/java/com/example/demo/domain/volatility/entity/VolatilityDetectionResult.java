package com.example.demo.domain.volatility.entity;

import java.time.LocalDate;
import java.util.List;

/**
 * 변동성 탐지 1회의 결과.
 * 거래일과 유니버스 크기를 KRX 응답에서 그대로 실어와, 저장 날짜가 서버 시각에,
 * 시총 가중치의 분모가 하드코딩 상수에 의존하지 않게 한다.
 */
public record VolatilityDetectionResult(
        LocalDate tradeDate,
        int universeSize,
        List<VolatilitySignal> signals
) {
}
