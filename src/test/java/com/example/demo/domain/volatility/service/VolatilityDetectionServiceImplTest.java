package com.example.demo.domain.volatility.service;

import com.example.demo.api.krx.dto.KrxOhlcvResponseDto;
import com.example.demo.api.krx.service.KrxService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * top10 선정 로직(selectTop)과 KRX Lambda 응답을 지표 계산으로 넘기는 오케스트레이션(detect) 유닛테스트.
 * OHLCV 정제(0값 제거)·60거래일 트리밍은 KRX Lambda(Python) 책임으로 이관됐으므로 여기서는 검증하지 않는다.
 */
class VolatilityDetectionServiceImplTest {

    private static final int UNIVERSE_SIZE = 100;

    private final KrxService krxService = Mockito.mock(KrxService.class);
    private final VolatilityDetectionService service = new VolatilityDetectionServiceImpl(krxService);

    @Test
    void alert가_아닌_종목은_후보에서_제외() {
        VolatilitySignal alerted = signal("005930", 1, 0.5, true);
        VolatilitySignal notAlerted = signal("000660", 2, 0.9, false);

        List<VolatilitySignal> top = service.selectTop(List.of(alerted, notAlerted), UNIVERSE_SIZE, 10);

        assertThat(top).containsExactly(alerted);
    }

    @Test
    void 후보가_10개_미만이면_있는_만큼만_반환() {
        List<VolatilitySignal> signals = List.of(
                signal("A", 1, 0.9, true),
                signal("B", 2, 0.8, true),
                signal("C", 3, 0.7, true)
        );

        List<VolatilitySignal> top = service.selectTop(signals, UNIVERSE_SIZE, 10);

        assertThat(top).hasSize(3);
    }

    @Test
    void 후보가_10개_초과면_상위_10개만_반환() {
        List<VolatilitySignal> signals = IntStream.rangeClosed(1, 15)
                .mapToObj(i -> signal("CODE" + i, i, i / 15.0, true))
                .collect(Collectors.toList());

        List<VolatilitySignal> top = service.selectTop(signals, UNIVERSE_SIZE, 10);

        assertThat(top).hasSize(10);
    }

    @Test
    void combinedScore는_변동성점수0_7_시총가중치0_3으로_정렬된다() {
        // 시총 1위(rank=1, marketCapWeight=1.0)이지만 score가 아주 낮은 종목
        VolatilitySignal highCapLowScore = signal("HIGHCAP", 1, 0.05, true);
        // 시총 100위(rank=100, marketCapWeight=0.01)이지만 score가 매우 높은 종목
        VolatilitySignal lowCapHighScore = signal("LOWCAP", 100, 0.9, true);

        // combinedScore(highCapLowScore) = 0.7*0.05 + 0.3*1.00 = 0.335
        // combinedScore(lowCapHighScore)  = 0.7*0.90 + 0.3*0.01 = 0.633
        List<VolatilitySignal> top = service.selectTop(
                List.of(highCapLowScore, lowCapHighScore), UNIVERSE_SIZE, 10);

        assertThat(top).containsExactly(lowCapHighScore, highCapLowScore);
    }

    @Test
    void detect는_KRX_Lambda_응답의_각_종목을_지표계산으로_넘겨_신호를_만든다() throws Exception {
        Mockito.when(krxService.getTopMarketCapOhlcv(ArgumentMatchers.anyInt()))
                .thenReturn(ohlcvResponse(stockJson("005930", "삼성전자", 1, syntheticCloses(60))));

        List<VolatilitySignal> detected = service.detect(1);

        assertThat(detected).hasSize(1);
        VolatilitySignal signal = detected.get(0);
        assertThat(signal.stockCode()).isEqualTo("005930");
        assertThat(signal.stockName()).isEqualTo("삼성전자");
        assertThat(signal.marketCapRank()).isEqualTo(1);
        assertThat(signal.dailyReturnPct()).isNotNaN();
    }

    @Test
    void 한_종목_계산이_실패해도_나머지_종목은_정상_처리된다() throws Exception {
        // 60개 미만 배열은 VolatilityIndicatorCalculator.calculate()가 IllegalArgumentException을 던짐
        String broken = stockJson("000660", "SK하이닉스", 2, syntheticCloses(5));
        String valid = stockJson("005930", "삼성전자", 1, syntheticCloses(60));
        Mockito.when(krxService.getTopMarketCapOhlcv(ArgumentMatchers.anyInt()))
                .thenReturn(ohlcvResponse(broken, valid));

        List<VolatilitySignal> detected = service.detect(2);

        assertThat(detected).hasSize(1);
        assertThat(detected.get(0).stockCode()).isEqualTo("005930");
    }

    private VolatilitySignal signal(String stockCode, int rank, double score, boolean alert) {
        return new VolatilitySignal(
                stockCode, stockCode + "_name", rank,
                0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                score, alert, List.of()
        );
    }

    private double[] syntheticCloses(int n) {
        double[] close = new double[n];
        for (int i = 0; i < n; i++) {
            close[i] = 1000.0 + (i % 5) * 4.0;
        }
        return close;
    }

    private String stockJson(String stockCode, String stockName, int rank, double[] close) {
        String closeJson = java.util.Arrays.stream(close)
                .mapToObj(String::valueOf)
                .collect(Collectors.joining(","));
        return String.format(
                "{\"stock_code\":\"%s\",\"stock_name\":\"%s\",\"market_cap_rank\":%d,"
                        + "\"close\":[%s],\"high\":[%s],\"low\":[%s],\"volume\":[%s]}",
                stockCode, stockName, rank, closeJson, closeJson, closeJson, closeJson);
    }

    private KrxOhlcvResponseDto ohlcvResponse(String... stockJsonEntries) throws Exception {
        String json = "{\"effective_trade_date\":\"20260731\",\"universe_size\":100,"
                + "\"stocks\":[" + String.join(",", stockJsonEntries) + "],\"errors\":[]}";
        return new ObjectMapper().readValue(json, KrxOhlcvResponseDto.class);
    }
}
