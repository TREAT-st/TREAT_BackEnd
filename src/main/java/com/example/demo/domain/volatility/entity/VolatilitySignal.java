package com.example.demo.domain.volatility.entity;

import java.util.List;

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
