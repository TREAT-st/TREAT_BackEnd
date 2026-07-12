package com.example.demo.domain.prediction.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PredictionTarget {

    UNDER_3("3% 미만 상승", null, 3.0, 1),
    BETWEEN_3_5("3%이상 - 5%미만 상승", 3.0, 5.0, 2),
    OVER_5("5%이상 급상승", 5.0, null, 3);

    private final String description;
    private final Double minRate;  // 하한(포함). null이면 하한 없음
    private final Double maxRate;  // 상한(미포함). null이면 상한 없음
    private final int weight;      // 목표치 높을수록 가중치 ↑

    /**
     * 실제 등락률이 이 구간에 들어오면 적중.
     * 화면 기준: 세 구간 모두 "상승" 예측이므로 상승분(양수) 기준으로 판정.
     */
    public boolean matches(double actualRate) {
        boolean overLower = (minRate == null) || actualRate >= minRate;
        boolean underUpper = (maxRate == null) || actualRate < maxRate;
        return overLower && underUpper;
    }
}
