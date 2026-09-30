package com.example.demo.domain.volatility.service;

import com.example.demo.api.krx.dto.KrxOhlcvResponseDto;
import com.example.demo.api.krx.service.KrxService;
import com.example.demo.domain.volatility.entity.VolatilityDetectionResult;
import com.example.demo.domain.volatility.entity.VolatilitySignal;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.demo.common.exception.GeneralException;
import com.example.demo.domain.volatility.exception.VolatilityErrorStatus;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    void combinedScore는_변동성점수0_85_시총가중치0_15로_정렬된다() {
        // 시총 1위(rank=1, marketCapWeight=1.0)이지만 조건 1개만 걸린 종목(score 0.30)
        VolatilitySignal highCapLowScore = signal("HIGHCAP", 1, 0.30, true);
        // 시총 100위(rank=100, marketCapWeight=0.01)이지만 조건 3개가 걸린 종목(score 0.75)
        VolatilitySignal lowCapHighScore = signal("LOWCAP", 100, 0.75, true);

        // combinedScore(highCapLowScore) = 0.85*0.30 + 0.15*1.00 = 0.405
        // combinedScore(lowCapHighScore) = 0.85*0.75 + 0.15*0.01 = 0.639
        // 가중치가 0.7/0.3이던 시절에는 각각 0.510 / 0.528로 사실상 동점이라,
        // 대형주가 조건 하나만 걸려도 조건 셋을 만족한 소형주와 맞먹었다.
        List<VolatilitySignal> top = service.selectTop(
                List.of(highCapLowScore, lowCapHighScore), UNIVERSE_SIZE, 10);

        assertThat(top).containsExactly(lowCapHighScore, highCapLowScore);
    }

    /**
     * universeSize를 응답의 universe_size 대신 수신 건수로 대체한 경우, 시총 순위가
     * universeSize를 넘을 수 있다. 가중치가 음수가 되면 같은 변동성 점수인데도 점수가 깎인다.
     */
    @Test
    void 시총순위가_universeSize보다_커도_가중치는_음수가_되지_않는다() {
        // universeSize=90, rank=100 -> 가중치 (90-100+1)/90 = -0.1
        VolatilitySignal outOfRange = signal("OUTRANGE", 100, 0.15, true);
        // universeSize=90, rank=70  -> 가중치 (90-70+1)/90 = 0.2333
        VolatilitySignal inRange = signal("INRANGE", 70, 0.10, true);

        // clamp 있음: OUTRANGE = 0.85*0.15 + 0.15*0     = 0.1275  > INRANGE = 0.120
        // clamp 없음: OUTRANGE = 0.85*0.15 + 0.15*(-0.1) = 0.1125  < INRANGE = 0.120
        // 즉 음수 가중치를 막지 않으면 순위가 뒤집힌다.
        List<VolatilitySignal> top = service.selectTop(List.of(outOfRange, inRange), 90, 10);

        assertThat(top).containsExactly(outOfRange, inRange);
    }

    @Test
    void detect는_KRX_Lambda_응답의_각_종목을_지표계산으로_넘겨_신호를_만든다() throws Exception {
        Mockito.when(krxService.getTopMarketCapOhlcv(ArgumentMatchers.anyInt()))
                .thenReturn(ohlcvResponse(stockJson("005930", "삼성전자", 1, syntheticCloses(60))));

        VolatilityDetectionResult result = service.detect(1);

        // 거래일과 유니버스 크기는 응답(effective_trade_date, universe_size)에서 그대로 실려 온다.
        assertThat(result.tradeDate()).isEqualTo(LocalDate.of(2026, 7, 31));
        assertThat(result.universeSize()).isEqualTo(100);

        List<VolatilitySignal> detected = result.signals();
        assertThat(detected).hasSize(1);
        VolatilitySignal signal = detected.get(0);
        assertThat(signal.stockCode()).isEqualTo("005930");
        assertThat(signal.stockName()).isEqualTo("삼성전자");
        assertThat(signal.marketCapRank()).isEqualTo(1);
        assertThat(signal.dailyReturnPct()).isNotNaN();
    }

    /**
     * 종목별 실패는 스킵으로 넘기지만, 너무 많이 빠지면 상위 10종목이 표본 부족으로 왜곡된다.
     * 그대로 저장하면 그날 기존 기록이 적은 결과로 교체되면서 reportUrl까지 사라진다.
     */
    @Test
    void 분석_성공_종목이_기준_미만이면_탐지를_중단한다() throws Exception {
        // 5종목 수신, 그중 3종목이 데이터 부족 → 성공 2/5 = 40% < 80%
        Mockito.when(krxService.getTopMarketCapOhlcv(ArgumentMatchers.anyInt()))
                .thenReturn(ohlcvResponse(
                        stockJson("005930", "삼성전자", 1, syntheticCloses(60)),
                        stockJson("000660", "SK하이닉스", 2, syntheticCloses(60)),
                        stockJson("035420", "NAVER", 3, syntheticCloses(5)),
                        stockJson("051910", "LG화학", 4, syntheticCloses(5)),
                        stockJson("006400", "삼성SDI", 5, syntheticCloses(5))));

        assertThatThrownBy(() -> service.detect(5))
                .isInstanceOf(GeneralException.class)
                .extracting(e -> ((GeneralException) e).getErrorReasonHttpStatus().getCode())
                .isEqualTo(VolatilityErrorStatus.VOLATILITY_DETECTION_FAILED.getCode());
    }

    @Test
    void 스킵이_기준_이내면_실패_종목만_빼고_진행한다() throws Exception {
        // 5종목 중 1종목만 데이터 부족 → 성공 4/5 = 80% >= 80%
        Mockito.when(krxService.getTopMarketCapOhlcv(ArgumentMatchers.anyInt()))
                .thenReturn(ohlcvResponse(
                        stockJson("005930", "삼성전자", 1, syntheticCloses(60)),
                        stockJson("000660", "SK하이닉스", 2, syntheticCloses(60)),
                        stockJson("035420", "NAVER", 3, syntheticCloses(60)),
                        stockJson("051910", "LG화학", 4, syntheticCloses(60)),
                        stockJson("006400", "삼성SDI", 5, syntheticCloses(5))));

        assertThat(service.detect(5).signals())
                .extracting(VolatilitySignal::stockCode)
                .containsExactlyInAnyOrder("005930", "000660", "035420", "051910");
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
