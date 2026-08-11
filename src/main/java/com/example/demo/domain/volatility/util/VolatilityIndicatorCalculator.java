package com.example.demo.domain.volatility.util;

/**
 * 변동성 지표 순수 계산 유틸.
 * 네트워크/DB 의존 없음 — volatility_detector/indicators.py 포팅.
 * 입력 배열은 날짜 오름차순(index 0이 가장 과거)이어야 한다.
 */
public class VolatilityIndicatorCalculator {

    static final int ROLLING_WINDOW = 20;
    private static final double TRADING_DAYS_PER_YEAR = 252.0;

    /** calculate()가 요구하는 최소 유효 거래일 수. 오케스트레이션 단계의 스킵 기준으로 재사용.
     * ROLLING_WINDOW + 2: dailyReturns()가 n-1개를 생성하고, percentileRank()는 2개 이상 필요. */
    public static final int MIN_VALID_TRADING_DAYS = ROLLING_WINDOW + 2;

    private VolatilityIndicatorCalculator() {
    }

    public record IndicatorSnapshot(
            double dailyReturnPct,
            double intradayRangePct,
            double vol20AnnualizedPct,
            double bbPercentB,
            double bbWidthPct,
            double volumeSpikeRatio,
            double vol20Quantile
    ) {
    }

    public static IndicatorSnapshot calculate(double[] close, double[] high, double[] low, double[] volume) {
        if (close == null || high == null || low == null || volume == null) {
            throw new IllegalArgumentException("OHLCV 배열이 null입니다.");
        }
        int n = close.length;
        if (n < MIN_VALID_TRADING_DAYS) {
            throw new IllegalArgumentException("최소 " + MIN_VALID_TRADING_DAYS + "개의 거래일 데이터가 필요합니다.");
        }
        if (high.length != n || low.length != n || volume.length != n) {
            throw new IllegalArgumentException(
                    "OHLCV 배열 길이가 일치하지 않습니다. close=" + n
                            + " high=" + high.length + " low=" + low.length + " volume=" + volume.length);
        }

        double[] returns = dailyReturns(close);
        double[] vol20Series = rollingAnnualizedVol(returns);

        return new IndicatorSnapshot(
                dailyReturnPct(close),
                intradayRangePct(high[n - 1], low[n - 1], close[n - 1]),
                vol20Series[vol20Series.length - 1],
                bbPercentB(close),
                bbWidthPct(close),
                volumeSpikeRatio(volume),
                percentileRank(vol20Series, vol20Series[vol20Series.length - 1])
        );
    }

    /** 마지막 거래일의 일간수익률(%). close.length >= 2 필요. */
    static double dailyReturnPct(double[] close) {
        int n = close.length;
        return (close[n - 1] / close[n - 2] - 1.0) * 100.0;
    }

    /** 당일 고가-저가 변동폭(%). */
    static double intradayRangePct(double high, double low, double close) {
        return (high - low) / close * 100.0;
    }

    /** 최근 20일 종가 기준 볼린저밴드 %B. close.length >= ROLLING_WINDOW 필요. */
    static double bbPercentB(double[] close) {
        int n = close.length;
        double ma20 = mean(close, n - ROLLING_WINDOW, n);
        double std20 = stddev(close, n - ROLLING_WINDOW, n);
        double upper = ma20 + 2.0 * std20;
        double lower = ma20 - 2.0 * std20;
        double band = upper - lower;
        return band == 0.0 ? Double.NaN : (close[n - 1] - lower) / band;
    }

    /** 최근 20일 종가 기준 볼린저밴드 폭(%). close.length >= ROLLING_WINDOW 필요. */
    static double bbWidthPct(double[] close) {
        int n = close.length;
        double ma20 = mean(close, n - ROLLING_WINDOW, n);
        double std20 = stddev(close, n - ROLLING_WINDOW, n);
        double band = 4.0 * std20;
        return ma20 == 0.0 ? Double.NaN : band / ma20 * 100.0;
    }

    /** 최근 20일 평균 거래량 대비 당일 거래량 배수. volume.length >= ROLLING_WINDOW 필요. */
    static double volumeSpikeRatio(double[] volume) {
        int n = volume.length;
        double volMa20 = mean(volume, n - ROLLING_WINDOW, n);
        return volMa20 == 0.0 ? Double.NaN : volume[n - 1] / volMa20;
    }

    static double[] dailyReturns(double[] close) {
        double[] returns = new double[close.length - 1];
        for (int i = 1; i < close.length; i++) {
            returns[i - 1] = close[i] / close[i - 1] - 1.0;
        }
        return returns;
    }

    /** returns 배열에 20일 롤링 표준편차를 연환산(%)해 시계열로 반환. 윈도우를 못 채우면 NaN 1개짜리 배열. */
    static double[] rollingAnnualizedVol(double[] returns) {
        int count = returns.length - ROLLING_WINDOW + 1;
        if (count < 1) {
            return new double[]{Double.NaN};
        }
        double[] result = new double[count];
        for (int i = 0; i < count; i++) {
            double std = stddev(returns, i, i + ROLLING_WINDOW);
            result[i] = std * Math.sqrt(TRADING_DAYS_PER_YEAR) * 100.0;
        }
        return result;
    }

    private static double mean(double[] arr, int fromInclusive, int toExclusive) {
        double sum = 0.0;
        int len = toExclusive - fromInclusive;
        for (int i = fromInclusive; i < toExclusive; i++) {
            sum += arr[i];
        }
        return sum / len;
    }

    /** 표본표준편차 (ddof=1, pandas 기본값과 동일). */
    private static double stddev(double[] arr, int fromInclusive, int toExclusive) {
        int len = toExclusive - fromInclusive;
        if (len < 2) {
            return 0.0;
        }
        double m = mean(arr, fromInclusive, toExclusive);
        double sumSq = 0.0;
        for (int i = fromInclusive; i < toExclusive; i++) {
            double diff = arr[i] - m;
            sumSq += diff * diff;
        }
        return Math.sqrt(sumSq / (len - 1));
    }

    /**
     * pandas {@code Series.rank(pct=True)}의 근사치 — 동률은 평균 순위로 처리.
     * series 원소가 2개 미만이거나 latest가 NaN이면 순위가 정의되지 않아 NaN 반환.
     */
    static double percentileRank(double[] series, double latest) {
        if (Double.isNaN(latest)) {
            return Double.NaN;
        }
        int less = 0;
        int equal = 0;
        int validCount = 0;
        for (double v : series) {
            if (Double.isNaN(v)) {
                continue;
            }
            validCount++;
            if (v < latest) {
                less++;
            } else if (v == latest) {
                equal++;
            }
        }
        if (validCount < 2) {
            return Double.NaN;
        }
        double avgRank = less + (equal + 1) / 2.0;
        return avgRank / validCount;
    }
}
