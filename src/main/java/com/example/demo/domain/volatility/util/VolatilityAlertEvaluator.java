package com.example.demo.domain.volatility.util;

import com.example.demo.domain.volatility.util.VolatilityIndicatorCalculator.IndicatorSnapshot;

import java.util.ArrayList;
import java.util.List;

public class VolatilityAlertEvaluator {

    public static final String REASON_DAILY_RETURN = "daily_return_abs_ge_5";
    public static final String REASON_INTRADAY_RANGE = "intraday_range_ge_5";
    public static final String REASON_VOLUME_SPIKE = "volume_spike_ge_2";
    public static final String REASON_BB_HIGH = "bb_percent_b_ge_0.9";
    public static final String REASON_BB_LOW = "bb_percent_b_le_0.1";
    public static final String REASON_VOL20_QUANTILE = "vol20_quantile_ge_0.8";

    private static final double DAILY_RETURN_ABS_THRESHOLD = 5.0;
    private static final double INTRADAY_RANGE_THRESHOLD = 5.0;
    private static final double VOLUME_SPIKE_THRESHOLD = 2.0;
    private static final double BB_PERCENT_B_HIGH = 0.9;
    private static final double BB_PERCENT_B_LOW = 0.1;
    private static final double VOL20_QUANTILE_THRESHOLD = 0.8;

    // 지표가 "변동성 자체"를 얼마나 직접 반영하는지 기준으로 부여한 가중치. 합계 1.0.
    private static final double WEIGHT_VOL20_QUANTILE = 0.30;
    private static final double WEIGHT_INTRADAY_RANGE = 0.25;
    private static final double WEIGHT_VOLUME_SPIKE = 0.20;
    private static final double WEIGHT_DAILY_RETURN = 0.15;
    private static final double WEIGHT_BB_EXTREME = 0.10;

    private VolatilityAlertEvaluator() {
    }

    public record AlertResult(double score, boolean alert, List<String> reasons) {
    }

    public static AlertResult evaluate(IndicatorSnapshot snapshot) {
        List<String> reasons = new ArrayList<>();
        double score = 0.0;

        if (!Double.isNaN(snapshot.dailyReturnPct())
                && Math.abs(snapshot.dailyReturnPct()) >= DAILY_RETURN_ABS_THRESHOLD) {
            reasons.add(REASON_DAILY_RETURN);
            score += WEIGHT_DAILY_RETURN;
        }

        if (!Double.isNaN(snapshot.intradayRangePct())
                && snapshot.intradayRangePct() >= INTRADAY_RANGE_THRESHOLD) {
            reasons.add(REASON_INTRADAY_RANGE);
            score += WEIGHT_INTRADAY_RANGE;
        }

        if (!Double.isNaN(snapshot.volumeSpikeRatio())
                && snapshot.volumeSpikeRatio() >= VOLUME_SPIKE_THRESHOLD) {
            reasons.add(REASON_VOLUME_SPIKE);
            score += WEIGHT_VOLUME_SPIKE;
        }

        // bb_percent_b 상단/하단은 같은 지표의 양끝단이라 가중치는 한 번만 반영.
        boolean bbExtreme = false;
        if (!Double.isNaN(snapshot.bbPercentB())) {
            if (snapshot.bbPercentB() >= BB_PERCENT_B_HIGH) {
                reasons.add(REASON_BB_HIGH);
                bbExtreme = true;
            }
            if (snapshot.bbPercentB() <= BB_PERCENT_B_LOW) {
                reasons.add(REASON_BB_LOW);
                bbExtreme = true;
            }
        }
        if (bbExtreme) {
            score += WEIGHT_BB_EXTREME;
        }

        if (!Double.isNaN(snapshot.vol20Quantile())
                && snapshot.vol20Quantile() >= VOL20_QUANTILE_THRESHOLD) {
            reasons.add(REASON_VOL20_QUANTILE);
            score += WEIGHT_VOL20_QUANTILE;
        }

        boolean alert = !reasons.isEmpty();
        return new AlertResult(score, alert, reasons);
    }
}
