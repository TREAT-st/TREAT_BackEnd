package com.example.demo.api.stockDailyPrice.service;

import com.example.demo.api.stockDailyPrice.dto.StockDailyPriceResponseDto.ChartCandleResponse;
import com.example.demo.api.stockDailyPrice.dto.StockDailyPriceResponseDto.StockChartResponse;
import com.example.demo.common.exception.GeneralException;
import com.example.demo.domain.stockDailyPrice.entity.ChartRange;
import com.example.demo.domain.stock.entity.Stock;
import com.example.demo.domain.stockDailyPrice.entity.StockDailyPrice;
import com.example.demo.domain.stock.exception.StockHandler;
import com.example.demo.domain.stock.service.StockQueryService;
import com.example.demo.domain.stockDailyPrice.service.StockDailyPriceQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/** 종목 차트 조회. KRX는 부르지 않고 쌓인 일봉만 읽는다. */
class StockChartUseCaseTest {

    private static final LocalDate LAST = LocalDate.of(2026, 10, 16);   // 금

    private final StockQueryService stockQueryService = Mockito.mock(StockQueryService.class);
    private final StockDailyPriceQueryService stockDailyPriceQueryService = Mockito.mock(StockDailyPriceQueryService.class);

    private final StockChartUseCase useCase = new StockChartUseCase(stockQueryService, stockDailyPriceQueryService);

    @BeforeEach
    void setUp() {
        Mockito.when(stockQueryService.getStockByCode("005930"))
                .thenReturn(Stock.builder().stockCode("005930").stockName("삼성전자").build());
    }

    private StockDailyPrice day(LocalDate date, int close) {
        return StockDailyPrice.builder().stockCode("005930").tradeDate(date)
                .openPrice(BigDecimal.valueOf(close)).highPrice(BigDecimal.valueOf(close))
                .lowPrice(BigDecimal.valueOf(close)).closePrice(BigDecimal.valueOf(close)).build();
    }

    /** 기간의 끝은 오늘이 아니라 그 종목의 마지막 일봉이다. 휴장일이나 동기화 직전에도 1W가 비지 않는다. */
    @Test
    void 마지막_일봉을_끝으로_기간을_잡는다() {
        Mockito.when(stockDailyPriceQueryService.getLatestDailyPriceDate("005930")).thenReturn(Optional.of(LAST));
        Mockito.when(stockDailyPriceQueryService.getDailyPrices("005930", LocalDate.of(2026, 10, 9), LAST))
                .thenReturn(List.of(day(LAST.minusDays(1), 100), day(LAST, 105)));

        StockChartResponse response = useCase.getStockChart("005930", ChartRange.ONE_WEEK);

        assertThat(response.getStockName()).isEqualTo("삼성전자");
        assertThat(response.getRange()).isEqualTo("1W");
        assertThat(response.getInterval()).isEqualTo("day");
        assertThat(response.getData()).extracting(ChartCandleResponse::getTradeDate)
                .containsExactly(LAST.minusDays(1), LAST);
    }

    /** 모든 기간이 일봉이다. 1Y도 묶지 않고 일봉을 그대로 돌려준다. 예) 끝 10/16 → 2025-10-16 ~ 2026-10-16. */
    @Test
    void 일년도_일봉_그대로_끝_날짜의_1년_전_같은_날부터() {
        Mockito.when(stockDailyPriceQueryService.getLatestDailyPriceDate("005930")).thenReturn(Optional.of(LAST));
        Mockito.when(stockDailyPriceQueryService.getDailyPrices("005930", LocalDate.of(2025, 10, 16), LAST))
                .thenReturn(List.of(day(LocalDate.of(2025, 10, 16), 90), day(LAST.minusDays(4), 100), day(LAST, 105)));

        StockChartResponse response = useCase.getStockChart("005930", ChartRange.ONE_YEAR);

        assertThat(response.getRange()).isEqualTo("1Y");
        assertThat(response.getInterval()).isEqualTo("day");
        assertThat(response.getData()).extracting(ChartCandleResponse::getTradeDate)
                .containsExactly(LocalDate.of(2025, 10, 16), LAST.minusDays(4), LAST);
        assertThat(response.getData().get(2).getOpenPrice()).isEqualByComparingTo("105");
    }

    /** 백필 전처럼 일봉이 하나도 없으면 빈 배열이다. 종목은 있으니 404가 아니다. */
    @Test
    void 일봉이_없으면_빈_배열() {
        Mockito.when(stockDailyPriceQueryService.getLatestDailyPriceDate("005930")).thenReturn(Optional.empty());

        StockChartResponse response = useCase.getStockChart("005930", ChartRange.THREE_MONTHS);

        assertThat(response.getData()).isEmpty();
        verify(stockDailyPriceQueryService, never()).getDailyPrices(any(), any(), any());
    }

    @Test
    void 없는_종목은_404() {
        Mockito.when(stockQueryService.getStockByCode("999999")).thenThrow(StockHandler.notFound());

        assertThatThrownBy(() -> useCase.getStockChart("999999", ChartRange.ONE_MONTH))
                .isInstanceOf(GeneralException.class);
    }

    /** 앱 사용량이 KRX 호출로 이어지지 않는 것이 이 구조의 목적이다. 유스케이스가 KRX에 의존하지 않는 것으로 보장되므로, 여기서는 일봉 조회만 쓰는지 본다. */
    @Test
    void 차트_조회는_쌓인_일봉만_읽는다() {
        Mockito.when(stockDailyPriceQueryService.getLatestDailyPriceDate("005930")).thenReturn(Optional.of(LAST));
        Mockito.when(stockDailyPriceQueryService.getDailyPrices(any(), any(), any())).thenReturn(List.of(day(LAST, 105)));

        useCase.getStockChart("005930", ChartRange.THREE_YEARS);

        verify(stockDailyPriceQueryService).getDailyPrices(eq("005930"), any(), eq(LAST));
    }
}
