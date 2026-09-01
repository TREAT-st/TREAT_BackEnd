package com.example.demo.domain.volatility.service;

import com.example.demo.api.krx.dto.KrxOhlcvResponseDto;
import com.example.demo.api.krx.service.KrxService;
import com.example.demo.domain.volatility.entity.VolatilityDetectionResult;
import com.example.demo.domain.volatility.entity.VolatilitySignal;
import com.example.demo.domain.volatility.exception.VolatilityHandler;
import com.example.demo.domain.volatility.util.VolatilityAlertEvaluator;
import com.example.demo.domain.volatility.util.VolatilityIndicatorCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static com.example.demo.common.consts.StaticVariable.SEOUL_ZONE;

@Slf4j
@Service
@RequiredArgsConstructor
public class VolatilityDetectionServiceImpl implements VolatilityDetectionService {
    /**
     * 시총 가중치는 소형주 독식을 막는 보정이지 순위 기준이 아니다.
     * 상위권 경쟁은 대체로 점수가 높은 종목들 사이에서 벌어지는데, 그 구간의 변동성 점수 폭은
     * 0.3 안팎인 반면 시총 가중치는 항상 0~1 전 구간을 쓴다. 0.3을 주면 정작 경쟁 구간에서
     * 시총이 변동성보다 큰 영향을 갖게 되므로, 보정 역할에 맞게 0.15로 낮춘다.
     */
    private static final double COMBINED_SCORE_WEIGHT = 0.85;
    private static final double MARKET_CAP_WEIGHT = 0.15;

    /**
     * 수신 종목 중 이 비율만큼은 분석에 성공해야 그 회차를 신뢰한다.
     * 종목별 실패는 스킵으로 넘기지만, 너무 많이 빠지면 상위 10종목 자체가 표본 부족으로 왜곡된다.
     * 그대로 저장하면 그날 기존 기록이 적은 수의 결과로 교체되면서 reportUrl까지 함께 사라진다.
     * "전 종목 실패"만 막는 가드로는 이 경우를 잡지 못한다.
     */
    private static final double MIN_ANALYZED_RATIO = 0.8;

    private final KrxService krxService;

    @Override
    public VolatilityDetectionResult detect(int topN) {
        KrxOhlcvResponseDto response = krxService.getTopMarketCapOhlcv(topN);

        List<VolatilitySignal> signals = new ArrayList<>();
        for (KrxOhlcvResponseDto.StockOhlcv stock : response.getStocks()) {
            try {
                signals.add(analyze(stock));
            } catch (Exception e) {
                log.warn("변동성 분석 실패, 종목 스킵. stockCode={}, reason={}", stock.getStockCode(), e.getMessage());
            }
        }

        int received = response.getStocks().size();
        int skipped = received - signals.size();
        if (skipped > 0) {
            log.warn("변동성 분석 스킵 {}건 / 수신 {}건 (요청 {}건)", skipped, received, topN);
        }

        if (signals.size() < received * MIN_ANALYZED_RATIO) {
            log.error("분석에 성공한 종목이 너무 적어 탐지를 중단합니다. 수신={} 분석={} 최소={}",
                    received, signals.size(), (int) Math.ceil(received * MIN_ANALYZED_RATIO));
            throw VolatilityHandler.volatilityDetectionFailed();
        }

        return new VolatilityDetectionResult(
                resolveTradeDate(response.getEffectiveTradeDate()),
                resolveUniverseSize(response.getUniverseSize(), received),
                signals);
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

    /**
     * 거래일은 KRX 응답을 기준으로 삼는다. 서버 시각을 쓰면 타임존이나 자정 경계에서
     * 저장된 날짜와 실제 거래일이 어긋난다.
     * 응답에 값이 없을 때만 서버 날짜로 대체하되, 조용히 넘어가지 않도록 경고를 남긴다.
     */
    private LocalDate resolveTradeDate(String effectiveTradeDate) {
        if (effectiveTradeDate != null && !effectiveTradeDate.isBlank()) {
            try {
                return LocalDate.parse(effectiveTradeDate, DateTimeFormatter.BASIC_ISO_DATE);
            } catch (DateTimeParseException e) {
                log.warn("effective_trade_date 파싱 실패. value={}", effectiveTradeDate);
            }
        } else {
            log.warn("KRX 응답에 effective_trade_date가 없습니다.");
        }

        LocalDate fallback = LocalDate.now(SEOUL_ZONE);
        log.warn("거래일을 서버 날짜로 대체합니다. tradeDate={}", fallback);
        return fallback;
    }

    /**
     * 시총 가중치의 분모. 요청한 수(topN)를 그대로 쓰면 Lambda가 그보다 적게 보냈을 때
     * 순위가 왜곡되므로, Lambda가 알려준 유니버스 크기를 쓰고 없으면 실제 수신 건수로 대체한다.
     */
    private int resolveUniverseSize(int universeSize, int receivedCount) {
        if (universeSize > 0) {
            return universeSize;
        }
        log.warn("KRX 응답의 universe_size가 유효하지 않습니다. 수신 건수로 대체합니다. received={}", receivedCount);
        return receivedCount;
    }

    private double combinedScore(VolatilitySignal signal, int universeSize) {
        // 0으로 나누면 NaN이 되고, NaN은 정렬에서 최대값으로 취급되어 엉뚱한 종목이 상위로 올라온다.
        if (universeSize <= 0) {
            return COMBINED_SCORE_WEIGHT * signal.score();
        }
        // 응답이 요청보다 적게 와서 universeSize를 수신 건수로 대체한 경우, 시총 순위가
        // universeSize를 넘을 수 있다. 그대로 두면 가중치가 음수가 되어 점수를 깎는다.
        double marketCapWeight = (universeSize - signal.marketCapRank() + 1) / (double) universeSize;
        double clamped = Math.max(0.0, Math.min(1.0, marketCapWeight));
        return COMBINED_SCORE_WEIGHT * signal.score() + MARKET_CAP_WEIGHT * clamped;
    }

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
