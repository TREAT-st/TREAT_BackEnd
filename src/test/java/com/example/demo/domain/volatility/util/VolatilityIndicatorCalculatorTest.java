package com.example.demo.domain.volatility.util;

import org.junit.jupiter.api.Test;

import static com.example.demo.domain.volatility.util.VolatilityIndicatorCalculator.MIN_VALID_TRADING_DAYS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    /**
     * 다른 지표들과 마찬가지로 0 나누기는 Infinity가 아니라 NaN으로 돌려준다.
     * Infinity는 평가 단계의 임계값 비교를 무조건 통과해버린다.
     */
    @Test
    void 종가가_0이면_intradayRangePct는_NaN() {
        assertThat(VolatilityIndicatorCalculator.intradayRangePct(110.0, 100.0, 0.0)).isNaN();
    }

    @Test
    void 전일_종가가_0이면_dailyReturnPct는_NaN() {
        assertThat(VolatilityIndicatorCalculator.dailyReturnPct(new double[]{100.0, 0.0, 105.0})).isNaN();
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
    void percentileRank_NaN_혼합_시계열은_유효_관측치_기준으로_계산() {
        // 유효 관측치 [1.0, 2.0] 중 2.0 → 1.0 (100th percentile)
        assertThat(VolatilityIndicatorCalculator.percentileRank(new double[]{Double.NaN, 1.0, 2.0}, 2.0))
                .isCloseTo(1.0, within(TOLERANCE));

        // 유효 관측치 [1.0, 2.0, 3.0] 중 2.0 → 중간 순위
        double expected = (1 + (1 + 1) / 2.0) / 3.0;
        assertThat(VolatilityIndicatorCalculator.percentileRank(new double[]{Double.NaN, 1.0, 2.0, 3.0}, 2.0))
                .isCloseTo(expected, within(TOLERANCE));
    }

    @Test
    void percentileRank_유효_관측치_2개_미만이면_NaN() {
        // NaN 제외 후 유효값 1개
        assertThat(VolatilityIndicatorCalculator.percentileRank(new double[]{Double.NaN, 3.0}, 3.0)).isNaN();
        // 전부 NaN
        assertThat(VolatilityIndicatorCalculator.percentileRank(new double[]{Double.NaN, Double.NaN}, 1.0)).isNaN();
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

    @Test
    void calculate_20개_입력은_예외() {
        double[] data = ascendingClose(20);
        assertThatThrownBy(() ->
                VolatilityIndicatorCalculator.calculate(data, data, data, uniformVolume(20, 1000.0)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void calculate_21개_입력은_예외() {
        double[] data = ascendingClose(21);
        assertThatThrownBy(() ->
                VolatilityIndicatorCalculator.calculate(data, data, data, uniformVolume(21, 1000.0)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void calculate_22개_입력은_vol20Quantile이_유효() {
        // MIN_VALID_TRADING_DAYS = 22: vol20Series 값 2개 → percentileRank 정의됨
        assertThat(MIN_VALID_TRADING_DAYS).isEqualTo(22);
        double[] data = ascendingClose(22);
        VolatilityIndicatorCalculator.IndicatorSnapshot snapshot =
                VolatilityIndicatorCalculator.calculate(data, data, data, uniformVolume(22, 1000.0));
        assertThat(snapshot.vol20AnnualizedPct()).isNotNaN();
        assertThat(snapshot.vol20Quantile()).isNotNaN();
        assertThat(snapshot.vol20Quantile()).isBetween(0.0, 1.0);
    }

    /**
     * vol20 백분위는 롤링 변동성 시계열 안에서의 순위라, 관측 기간이 곧 표본 수다.
     * 60거래일이면 표본 40개(그마저 19일씩 겹쳐 독립 관측은 3개 수준)라 백분위가 거칠다.
     * 252거래일(1년)이면 232개로 늘어 같은 지표가 훨씬 촘촘해진다.
     */
    @Test
    void 관측기간이_길수록_롤링변동성_표본수가_늘어난다() {
        assertThat(VolatilityIndicatorCalculator.rollingAnnualizedVol(new double[59])).hasSize(40);
        assertThat(VolatilityIndicatorCalculator.rollingAnnualizedVol(new double[251])).hasSize(232);
    }

    @Test
    void calculate_252거래일_입력도_동일하게_동작한다() {
        // 관측 기간을 늘려도 다른 지표는 끝에서 20일만 보므로 영향이 없고, vol20Quantile만 정밀해진다.
        double[] data = ascendingClose(252);
        VolatilityIndicatorCalculator.IndicatorSnapshot snapshot =
                VolatilityIndicatorCalculator.calculate(data, data, data, uniformVolume(252, 1000.0));

        assertThat(snapshot.dailyReturnPct()).isNotNaN();
        assertThat(snapshot.vol20AnnualizedPct()).isNotNaN();
        assertThat(snapshot.vol20Quantile()).isBetween(0.0, 1.0);
    }

    private static double[] ascendingClose(int n) {
        double[] close = new double[n];
        for (int i = 0; i < n; i++) {
            close[i] = i + 1.0;
        }
        return close;
    }

    private static double[] uniformVolume(int n, double value) {
        double[] volume = new double[n];
        for (int i = 0; i < n; i++) {
            volume[i] = value;
        }
        return volume;
    }
}
