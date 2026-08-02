package com.example.demo.domain.volatility.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * volatility_detector/tests/test_indicators.py의 대표 케이스를 이식한 유닛테스트.
 */
class VolatilityIndicatorCalculatorTest {

    private static final double TOLERANCE = 1e-9;

    @Test
    void dailyReturnPct_계산() {
        assertThat(VolatilityIndicatorCalculator.dailyReturnPct(new double[]{100.0, 105.0}))
                .isCloseTo(5.0, within(TOLERANCE));

        double expected = (99.0 - 105.0) / 105.0 * 100.0;
        assertThat(VolatilityIndicatorCalculator.dailyReturnPct(new double[]{100.0, 105.0, 99.0}))
                .isCloseTo(expected, within(TOLERANCE));
    }

    @Test
    void intradayRangePct_계산() {
        double expected = (110.0 - 100.0) / 105.0 * 100.0;
        assertThat(VolatilityIndicatorCalculator.intradayRangePct(110.0, 100.0, 105.0))
                .isCloseTo(expected, within(TOLERANCE));
    }

    @Test
    void bollingerPercentB_및_width_계산() {
        // 마지막 20개 종가 = 1..20 -> ma20=10.5, 표본표준편차=sqrt(35)
        double[] close = new double[20];
        for (int i = 0; i < 20; i++) {
            close[i] = i + 1.0;
        }

        double std = Math.sqrt(35.0);
        double upper = 10.5 + 2 * std;
        double lower = 10.5 - 2 * std;
        double expectedPb = (20.0 - lower) / (upper - lower);
        double expectedWidth = (upper - lower) / 10.5 * 100.0;

        assertThat(VolatilityIndicatorCalculator.bbPercentB(close)).isCloseTo(expectedPb, within(1e-6));
        assertThat(VolatilityIndicatorCalculator.bbWidthPct(close)).isCloseTo(expectedWidth, within(1e-6));
        assertThat(VolatilityIndicatorCalculator.bbPercentB(close)).isGreaterThanOrEqualTo(0.9);
    }

    @Test
    void volumeSpikeRatio_계산() {
        double[] volume = new double[20];
        for (int i = 0; i < 19; i++) {
            volume[i] = 100.0;
        }
        volume[19] = 300.0;

        // rolling(20) mean = (100*19 + 300)/20 = 110
        assertThat(VolatilityIndicatorCalculator.volumeSpikeRatio(volume))
                .isCloseTo(300.0 / 110.0, within(TOLERANCE));
    }

    @Test
    void percentileRank_극단값() {
        double[] asc = {1.0, 2.0, 3.0, 4.0, 5.0};
        double[] desc = {5.0, 4.0, 3.0, 2.0, 1.0};

        assertThat(VolatilityIndicatorCalculator.percentileRank(asc, asc[asc.length - 1]))
                .isCloseTo(1.0, within(TOLERANCE));
        assertThat(VolatilityIndicatorCalculator.percentileRank(desc, desc[desc.length - 1]))
                .isCloseTo(0.2, within(TOLERANCE));
    }

    @Test
    void percentileRank_원소가_2개_미만이면_NaN() {
        assertThat(VolatilityIndicatorCalculator.percentileRank(new double[]{3.0}, 3.0)).isNaN();
    }

    @Test
    void calculate_전체_스냅샷_라운드트립() {
        // 40일치 -> vol20 시계열이 2개 이상 나와 quantile이 정의됨
        double[] close = new double[40];
        for (int i = 0; i < 40; i++) {
            close[i] = i + 1.0;
        }

        VolatilityIndicatorCalculator.IndicatorSnapshot snapshot =
                VolatilityIndicatorCalculator.calculate(close, close, close, uniformVolume(40, 1000.0));

        assertThat(snapshot.dailyReturnPct()).isNotNaN();
        assertThat(snapshot.vol20Quantile()).isNotNaN();
        assertThat(snapshot.vol20Quantile()).isBetween(0.0, 1.0);
    }

    private static double[] uniformVolume(int n, double value) {
        double[] volume = new double[n];
        for (int i = 0; i < n; i++) {
            volume[i] = value;
        }
        return volume;
    }
}
