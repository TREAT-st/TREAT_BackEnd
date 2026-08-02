package com.example.demo.domain.volatility.util;

import com.example.demo.domain.volatility.util.VolatilityAlertEvaluator.AlertResult;
import com.example.demo.domain.volatility.util.VolatilityIndicatorCalculator.IndicatorSnapshot;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * volatility_detector/tests/test_indicators.py의 evaluate_alert 케이스를 이식 + 가중치 스코어 검증.
 */
class VolatilityAlertEvaluatorTest {

    @Test
    void 모든_조건_충족시_전체_reason과_만점_score() {
        IndicatorSnapshot snapshot = new IndicatorSnapshot(
                -6.0,   // abs >= 5
                7.0,    // >= 5
                30.0,
                0.95,   // >= 0.9
                10.0,
                3.0,    // >= 2
                0.85    // >= 0.8
        );

        AlertResult result = VolatilityAlertEvaluator.evaluate(snapshot);

        assertThat(result.reasons()).containsExactlyInAnyOrder(
                VolatilityAlertEvaluator.REASON_DAILY_RETURN,
                VolatilityAlertEvaluator.REASON_INTRADAY_RANGE,
                VolatilityAlertEvaluator.REASON_VOLUME_SPIKE,
                VolatilityAlertEvaluator.REASON_BB_HIGH,
                VolatilityAlertEvaluator.REASON_VOL20_QUANTILE
        );
        assertThat(result.alert()).isTrue();
        // 0.15+0.25+0.20+0.10+0.30 = 1.0 (bb 가중치는 한 번만 반영)
        assertThat(result.score()).isCloseTo(1.0, within(1e-9));
    }

    @Test
    void 하단_밴드_조건만_충족() {
        IndicatorSnapshot snapshot = new IndicatorSnapshot(
                1.0,
                1.0,
                10.0,
                0.05,  // <= 0.1
                10.0,
                1.0,
                0.5
        );

        AlertResult result = VolatilityAlertEvaluator.evaluate(snapshot);

        assertThat(result.reasons()).containsExactly(VolatilityAlertEvaluator.REASON_BB_LOW);
        assertThat(result.alert()).isTrue();
        assertThat(result.score()).isCloseTo(0.10, within(1e-9));
    }

    @Test
    void NaN이면_아무_조건도_충족하지_않음() {
        IndicatorSnapshot snapshot = new IndicatorSnapshot(
                Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN
        );

        AlertResult result = VolatilityAlertEvaluator.evaluate(snapshot);

        assertThat(result.reasons()).isEmpty();
        assertThat(result.alert()).isFalse();
        assertThat(result.score()).isZero();
    }

    @Test
    void bb_상단_하단_동시충족은_불가능하지만_가중치는_한번만() {
        // bb_percent_b가 상단(>=0.9) 조건만 만족하는 경우, 다른 조건 없이도 가중치 0.10만 반영되는지 확인
        IndicatorSnapshot snapshot = new IndicatorSnapshot(
                1.0, 1.0, 10.0, 0.95, 10.0, 1.0, 0.1
        );

        AlertResult result = VolatilityAlertEvaluator.evaluate(snapshot);

        assertThat(result.reasons()).containsExactly(VolatilityAlertEvaluator.REASON_BB_HIGH);
        assertThat(result.score()).isCloseTo(0.10, within(1e-9));
    }
}
