package com.example.demo.domain.volatility.service;

import java.util.List;

/**
 * 종목 하나에 대한 변동성 분석 결과. DB 엔티티가 아닌 순수 반환용 값 객체.
 * marketCapRank는 유니버스 조회 시의 시총 순위(1위부터)로, top10 결합 점수 계산에 쓰인다.
 */
public record VolatilitySignal(
        String stockCode,
        String stockName,
        int marketCapRank,
        double dailyReturnPct,
        double intradayRangePct,
        double vol20AnnualizedPct,
        double bbPercentB,
        double bbWidthPct,
        double volumeSpikeRatio,
        double vol20Quantile,
        double score,
        boolean alert,
        List<String> reasons
) {
}
