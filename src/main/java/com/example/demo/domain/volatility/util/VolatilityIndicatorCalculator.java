package com.example.demo.domain.volatility.util;

public class VolatilityIndicatorCalculator {

    static final int ROLLING_WINDOW = 20;
    private static final double TRADING_DAYS_PER_YEAR = 252.0;

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

    static double dailyReturnPct(double[] close) {
        int n = close.length;
        return (close[n - 1] / close[n - 2] - 1.0) * 100.0;
    }

    static double intradayRangePct(double high, double low, double close) {
        return (high - low) / close * 100.0;
    }

    static double bbPercentB(double[] close) {
        int n = close.length;
        double ma20 = mean(close, n - ROLLING_WINDOW, n);
        double std20 = stddev(close, n - ROLLING_WINDOW, n);
        double upper = ma20 + 2.0 * std20;
        double lower = ma20 - 2.0 * std20;
        double band = upper - lower;
        return band == 0.0 ? Double.NaN : (close[n - 1] - lower) / band;
    }

    static double bbWidthPct(double[] close) {
        int n = close.length;
        double ma20 = mean(close, n - ROLLING_WINDOW, n);
        double std20 = stddev(close, n - ROLLING_WINDOW, n);
        double band = 4.0 * std20;
        return ma20 == 0.0 ? Double.NaN : band / ma20 * 100.0;
    }

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
