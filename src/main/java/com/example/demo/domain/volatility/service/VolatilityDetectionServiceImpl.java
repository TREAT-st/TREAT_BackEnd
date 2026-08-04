package com.example.demo.domain.volatility.service;

import com.example.demo.api.krx.dto.KrxOhlcvResponseDto;
import com.example.demo.api.krx.service.KrxService;
import com.example.demo.domain.volatility.entity.VolatilitySignal;
import com.example.demo.domain.volatility.util.VolatilityAlertEvaluator;
import com.example.demo.domain.volatility.util.VolatilityIndicatorCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VolatilityDetectionServiceImpl implements VolatilityDetectionService {

    // score(변동성)와 marketCapWeight(시총)의 결합 비중. 변동성 리포트의 핵심 가치(뚜렷한 변동 우선)를 지키기 위해
    // 변동성 쪽에 더 큰 비중을 둔다.
    private static final double COMBINED_SCORE_WEIGHT = 0.7;
    private static final double MARKET_CAP_WEIGHT = 0.3;

    private final KrxService krxService;

    @Override
    public List<VolatilitySignal> detect(int topN) {
        KrxOhlcvResponseDto response = krxService.getTopMarketCapOhlcv(topN);

        List<VolatilitySignal> signals = new ArrayList<>();
        for (KrxOhlcvResponseDto.StockOhlcv stock : response.getStocks()) {
            try {
                signals.add(analyze(stock));
            } catch (Exception e) {
                log.warn("변동성 분석 실패, 종목 스킵. stockCode={}, reason={}", stock.getStockCode(), e.getMessage());
            }
        }
        return signals;
    }

    @Override
    public List<VolatilitySignal> selectTop(List<VolatilitySignal> signals, int universeSize, int topN) {
        return signals.stream()
                .filter(VolatilitySignal::alert)
                .sorted(Comparator.comparingDouble(
                        (VolatilitySignal s) -> combinedScore(s, universeSize)).reversed())
                .limit(topN)
                .collect(Collectors.toList());
    }

    private double combinedScore(VolatilitySignal signal, int universeSize) {
        double marketCapWeight = (universeSize - signal.marketCapRank() + 1) / (double) universeSize;
        return COMBINED_SCORE_WEIGHT * signal.score() + MARKET_CAP_WEIGHT * marketCapWeight;
    }

    /**
     * KRX Lambda가 이미 정제(0값 행 제거)·트리밍(최근 60거래일)까지 마친 배열을 내려주므로,
     * 추가 가공 없이 바로 지표 계산에 사용한다.
     */
    private VolatilitySignal analyze(KrxOhlcvResponseDto.StockOhlcv stock) {
        VolatilityIndicatorCalculator.IndicatorSnapshot snapshot = VolatilityIndicatorCalculator.calculate(
                stock.getClose(), stock.getHigh(), stock.getLow(), stock.getVolume());
        VolatilityAlertEvaluator.AlertResult alertResult = VolatilityAlertEvaluator.evaluate(snapshot);

        return new VolatilitySignal(
                stock.getStockCode(),
                stock.getStockName(),
                stock.getMarketCapRank(),
                snapshot.dailyReturnPct(),
                snapshot.intradayRangePct(),
                snapshot.vol20AnnualizedPct(),
                snapshot.bbPercentB(),
                snapshot.bbWidthPct(),
                snapshot.volumeSpikeRatio(),
                snapshot.vol20Quantile(),
                alertResult.score(),
                alertResult.alert(),
                alertResult.reasons()
        );
    }
}
