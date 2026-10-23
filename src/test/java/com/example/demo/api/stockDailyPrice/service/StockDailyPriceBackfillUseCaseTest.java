package com.example.demo.api.stockDailyPrice.service;

import com.example.demo.api.krx.dto.KrxDailyPricesByRangeResponseDto;
import com.example.demo.api.krx.service.KrxService;
import com.example.demo.api.stockDailyPrice.service.StockDailyPriceBackfillUseCase.BackfillPlan;
import com.example.demo.api.stockDailyPrice.service.StockDailyPriceBackfillUseCase.BackfillResult;
import com.example.demo.common.exception.BaseErrorCode;
import com.example.demo.common.exception.GeneralException;
import com.example.demo.domain.stock.service.StockQueryService;
import com.example.demo.domain.stockDailyPrice.entity.DailyPriceRow;
import com.example.demo.domain.stockDailyPrice.exception.StockDailyPriceErrorStatus;
import com.example.demo.domain.stockDailyPrice.service.StockDailyPriceCommandService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/** 3년치 백필: 대상·기간 검증, 20종목 묶음, 응답 검증, 실패·건너뜀 판정, 재시도. */
class StockDailyPriceBackfillUseCaseTest {

    private static final LocalDate LATEST = LocalDate.of(2026, 10, 16);
    private static final LocalDate THREE_YEARS_AGO = LocalDate.of(2023, 10, 16);

    private final KrxService krxService = Mockito.mock(KrxService.class);
    private final StockQueryService stockQueryService = Mockito.mock(StockQueryService.class);
    private final StockDailyPriceCommandService commandService = Mockito.mock(StockDailyPriceCommandService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final DailyPriceJobGuard guard = Mockito.mock(DailyPriceJobGuard.class);

    private final StockDailyPriceBackfillUseCase useCase = new StockDailyPriceBackfillUseCase(
            krxService, stockQueryService, new DailyPriceIngestor(commandService), guard);

    private static BackfillPlan plan(String... codes) {
        return new BackfillPlan(THREE_YEARS_AGO, LATEST, List.of(codes));
    }

    private static List<String> codes(int count) {
        return IntStream.range(0, count).mapToObj(i -> String.format("%06d", i + 1)).toList();
    }

    private void givenStocks(String... codes) {
        Mockito.when(stockQueryService.getLatestTradeDate()).thenReturn(Optional.of(LATEST));
        Mockito.when(stockQueryService.getAllStockCodes()).thenReturn(Arrays.asList(codes));
    }

    /** 각 종목에 일봉 한 행씩(10/16) 담긴 응답. */
    private KrxDailyPricesByRangeResponseDto response(List<String> codes) {
        String prices = codes.stream().map(code -> """
                { "stockCode": "%s", "rows": [
                  { "tradeDate": "20261016", "openPrice": 100, "highPrice": 110, "lowPrice": 90, "closePrice": 105 } ] }
                """.formatted(code)).collect(Collectors.joining(","));
        return parse("{ \"prices\": [" + prices + "] }");
    }

    private KrxDailyPricesByRangeResponseDto parse(String json) {
        try {
            return objectMapper.readValue(json, KrxDailyPricesByRangeResponseDto.class);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private void saveReturnsRowCount() {
        Mockito.when(commandService.saveDailyPrices(anyList()))
                .thenAnswer(invocation -> ((List<?>) invocation.getArgument(0)).size());
    }

    private void krxEchoesRequestedCodes() {
        Mockito.when(krxService.getDailyPricesByRange(any(), any(), anyList()))
                .thenAnswer(invocation -> response(invocation.getArgument(2)));
    }

    @SuppressWarnings("unchecked")
    private List<DailyPriceRow> allSavedRows() {
        ArgumentCaptor<List<DailyPriceRow>> captor = ArgumentCaptor.forClass(List.class);
        verify(commandService, Mockito.atLeastOnce()).saveDailyPrices(captor.capture());
        List<DailyPriceRow> rows = new ArrayList<>();
        captor.getAllValues().forEach(rows::addAll);
        return rows;
    }

    private void assertErrorCode(Runnable call, BaseErrorCode expected) {
        assertThatThrownBy(call::run)
                .isInstanceOf(GeneralException.class)
                .extracting(e -> ((GeneralException) e).getErrorReasonHttpStatus().getCode())
                .isEqualTo(expected.getReasonHttpStatus().getCode());
    }

    // ---------- 대상과 기간 ----------

    /** 3Y 차트([끝 − 3년, 끝])를 채운다. 양 끝 포함. */
    @Test
    void 기본은_최근_거래일부터_3년_전까지_전_종목() {
        givenStocks("000660", "005930");

        BackfillPlan plan = useCase.plan(null, null, null);

        assertThat(plan.from()).isEqualTo(THREE_YEARS_AGO);
        assertThat(plan.to()).isEqualTo(LATEST);
        assertThat(plan.stockCodes()).containsExactly("000660", "005930");
    }

    @Test
    void stockCodes를_지정하면_그_종목만_중복_없이_요청_순서대로() {
        givenStocks("000660", "005930", "035420");

        BackfillPlan plan = useCase.plan(List.of("035420", "005930", "035420"), null, null);

        assertThat(plan.stockCodes()).containsExactly("035420", "005930");
    }

    /** 오타를 조용히 건너뛰면 성공처럼 보인다. 하나라도 틀리면 전체를 거절한다. */
    @Test
    void 없는_종목코드가_있으면_4257() {
        givenStocks("005930");

        assertErrorCode(() -> useCase.plan(List.of("005930", "999999"), null, null), StockDailyPriceErrorStatus.DAILY_PRICE_UNKNOWN_STOCK_CODES);
    }

    @Test
    void 형식이_틀린_종목코드가_있으면_4257() {
        givenStocks("005930");

        assertErrorCode(() -> useCase.plan(List.of("5930"), null, null), StockDailyPriceErrorStatus.DAILY_PRICE_UNKNOWN_STOCK_CODES);
    }

    @Test
    void 반영된_시세가_없으면_4256() {
        Mockito.when(stockQueryService.getLatestTradeDate()).thenReturn(Optional.empty());

        assertErrorCode(() -> useCase.plan(null, null, null), StockDailyPriceErrorStatus.DAILY_PRICE_NOT_SYNCED_YET);
    }

    @Test
    void to가_최근_거래일보다_뒤면_4255() {
        givenStocks("005930");

        assertErrorCode(() -> useCase.plan(null, null, LATEST.plusDays(1)), StockDailyPriceErrorStatus.DAILY_PRICE_INVALID_RANGE);
    }

    @Test
    void from이_to보다_뒤면_4255() {
        givenStocks("005930");

        assertErrorCode(() -> useCase.plan(null, LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 1)),
                StockDailyPriceErrorStatus.DAILY_PRICE_INVALID_RANGE);
    }

    @Test
    void 지정한_기간을_그대로_쓴다() {
        givenStocks("005930");

        BackfillPlan plan = useCase.plan(null, LocalDate.of(2026, 1, 2), LocalDate.of(2026, 3, 31));

        assertThat(plan.from()).isEqualTo(LocalDate.of(2026, 1, 2));
        assertThat(plan.to()).isEqualTo(LocalDate.of(2026, 3, 31));
    }

    @Test
    void 종목이_없으면_빈_계획() {
        givenStocks();

        assertThat(useCase.plan(null, null, null).isEmpty()).isTrue();
    }

    // ---------- 묶음 ----------

    @Test
    void 종목을_20개씩_나눠_부른다() {
        List<String> all = codes(45);
        krxEchoesRequestedCodes();
        saveReturnsRowCount();

        BackfillResult result = useCase.backfill(new BackfillPlan(THREE_YEARS_AGO, LATEST, all), null);

        verify(krxService).getDailyPricesByRange(THREE_YEARS_AGO, LATEST, all.subList(0, 20));
        verify(krxService).getDailyPricesByRange(THREE_YEARS_AGO, LATEST, all.subList(20, 40));
        verify(krxService).getDailyPricesByRange(THREE_YEARS_AGO, LATEST, all.subList(40, 45));
        assertThat(result.totalStocks()).isEqualTo(45);
        assertThat(result.upsertedRows()).isEqualTo(45);
        assertThat(result.failedStockCodes()).isEmpty();
    }

    @Test
    void 묶음_하나가_실패해도_나머지는_저장한다() {
        List<String> all = codes(45);
        krxEchoesRequestedCodes();
        Mockito.when(krxService.getDailyPricesByRange(any(), any(), eq(all.subList(20, 40))))
                .thenThrow(new RuntimeException("Lambda timeout"));
        saveReturnsRowCount();

        BackfillResult result = useCase.backfill(new BackfillPlan(THREE_YEARS_AGO, LATEST, all), null);

        assertThat(result.upsertedRows()).isEqualTo(25);
        assertThat(result.failedStockCodes()).containsExactlyElementsOf(all.subList(20, 40));
    }

    // ---------- 실패·건너뜀 판정 ----------

    /** errors에 없어도 응답에서 빠진 종목은 실패다. */
    @Test
    void 응답에서_빠진_종목은_실패() {
        Mockito.when(krxService.getDailyPricesByRange(any(), any(), anyList())).thenReturn(response(List.of("005930")));
        saveReturnsRowCount();

        assertThat(useCase.backfill(plan("005930", "000660"), null).failedStockCodes()).containsExactly("000660");
    }

    /** 장기 거래정지처럼 기간 안에 일봉이 없는 것이 정상인 종목은 실패가 아니다. 매번 실패로 남으면 신호가 묻힌다. */
    @Test
    void skipped_종목은_실패가_아니다() {
        Mockito.when(krxService.getDailyPricesByRange(any(), any(), anyList())).thenReturn(parse("""
                { "prices": [ { "stockCode": "005930", "rows": [
                    { "tradeDate": "20261016", "openPrice": 100, "closePrice": 105 } ] } ],
                  "skipped": [ { "stockCode": "000660", "reason": "해당 기간 일봉 없음" } ] }
                """));
        saveReturnsRowCount();

        BackfillResult result = useCase.backfill(plan("005930", "000660"), null);

        assertThat(result.failedStockCodes()).isEmpty();
        assertThat(result.skippedStockCodes()).containsExactly("000660");
    }

    @Test
    void errors로_알린_종목은_실패() {
        Mockito.when(krxService.getDailyPricesByRange(any(), any(), anyList())).thenReturn(parse("""
                { "prices": [ { "stockCode": "005930", "rows": [
                    { "tradeDate": "20261016", "openPrice": 100, "closePrice": 105 } ] } ],
                  "errors": [ { "stockCode": "000660", "reason": "조회 실패" } ] }
                """));
        saveReturnsRowCount();

        assertThat(useCase.backfill(plan("005930", "000660"), null).failedStockCodes()).containsExactly("000660");
    }

    // ---------- 응답 검증 ----------

    @Test
    void 요청하지_않은_종목의_행은_저장하지_않는다() {
        Mockito.when(krxService.getDailyPricesByRange(any(), any(), anyList()))
                .thenReturn(response(List.of("005930", "999999")));
        saveReturnsRowCount();

        BackfillResult result = useCase.backfill(plan("005930"), null);

        assertThat(allSavedRows()).extracting(DailyPriceRow::stockCode).containsOnly("005930");
        assertThat(result.failedStockCodes()).isEmpty();
    }

    @Test
    void 기간_밖의_행은_저장하지_않고_그_종목은_실패로_남긴다() {
        Mockito.when(krxService.getDailyPricesByRange(any(), any(), anyList())).thenReturn(parse("""
                { "prices": [ { "stockCode": "005930", "rows": [
                    { "tradeDate": "20200101", "openPrice": 100, "closePrice": 105 },
                    { "tradeDate": "20261016", "openPrice": 102, "closePrice": 107 },
                    { "tradeDate": "20261019", "openPrice": 103, "closePrice": 108 } ] } ] }
                """));
        saveReturnsRowCount();

        BackfillResult result = useCase.backfill(plan("005930"), null);

        assertThat(allSavedRows()).extracting(DailyPriceRow::tradeDate).containsExactly(LATEST);
        assertThat(result.failedStockCodes()).containsExactly("005930");
    }

    /** 유효한 행이 저장되어도 버린 날은 드러나야 재백필할 수 있다. */
    @Test
    void 일부_행만_무효인_종목도_실패로_남긴다() {
        Mockito.when(krxService.getDailyPricesByRange(any(), any(), anyList())).thenReturn(parse("""
                { "prices": [
                  { "stockCode": "005930", "rows": [
                    { "tradeDate": "20261014", "openPrice": 100, "closePrice": 105 },
                    { "tradeDate": "20261015", "openPrice": 0, "closePrice": 106 },
                    { "tradeDate": "2026-10-16", "openPrice": 102, "closePrice": 107 } ] },
                  { "stockCode": "000660", "rows": [
                    { "tradeDate": "20261016", "openPrice": 200, "closePrice": 210 } ] } ] }
                """));
        saveReturnsRowCount();

        BackfillResult result = useCase.backfill(plan("005930", "000660"), null);

        assertThat(allSavedRows()).extracting(r -> r.stockCode() + ":" + r.tradeDate())
                .containsExactly("005930:2026-10-14", "000660:2026-10-16");
        assertThat(result.failedStockCodes()).containsExactly("005930");
    }

    @Test
    void 실패_종목은_중복_없이_요청_순서를_지킨다() {
        Mockito.when(krxService.getDailyPricesByRange(any(), any(), anyList())).thenReturn(parse("""
                { "prices": [
                  { "stockCode": "035420", "rows": [
                    { "tradeDate": "20261016", "openPrice": 300, "closePrice": 310 },
                    { "tradeDate": "20261015", "openPrice": -1, "closePrice": 300 } ] },
                  { "stockCode": "000660", "rows": [
                    { "tradeDate": "2026-10-16", "openPrice": 100, "closePrice": 105 },
                    { "tradeDate": "20200101", "openPrice": 100, "closePrice": 105 } ] } ] }
                """));
        saveReturnsRowCount();

        BackfillResult result = useCase.backfill(plan("005930", "000660", "035420"), null);

        assertThat(result.failedStockCodes()).containsExactly("005930", "000660", "035420");
    }

    // ---------- 동시 실행 ----------

    @Test
    void 유니크_충돌은_한_번_다시_시도한다() {
        Mockito.when(krxService.getDailyPricesByRange(any(), any(), anyList())).thenReturn(response(List.of("005930")));
        Mockito.when(commandService.saveDailyPrices(anyList()))
                .thenThrow(new DataIntegrityViolationException("uk_stock_daily_price_code_date"))
                .thenReturn(1);

        BackfillResult result = useCase.backfill(plan("005930"), null);

        verify(commandService, times(2)).saveDailyPrices(anyList());
        assertThat(result.upsertedRows()).isEqualTo(1);
        assertThat(result.failedStockCodes()).isEmpty();
    }

    @Test
    void 다시_시도해도_실패하면_그_묶음은_실패() {
        Mockito.when(krxService.getDailyPricesByRange(any(), any(), anyList())).thenReturn(response(List.of("005930")));
        Mockito.when(commandService.saveDailyPrices(anyList()))
                .thenThrow(new DataIntegrityViolationException("uk_stock_daily_price_code_date"));

        BackfillResult result = useCase.backfill(plan("005930"), null);

        verify(commandService, times(2)).saveDailyPrices(anyList());
        assertThat(result.failedStockCodes()).containsExactly("005930");
    }

    /** 계약 오류로 같은 종목이 prices와 errors에 함께 오면 성공으로 보면 안 된다. 저장하지 않고 실패로 남긴다. */
    @Test
    void errors와_prices에_함께_온_종목은_저장하지_않고_실패() {
        Mockito.when(krxService.getDailyPricesByRange(any(), any(), anyList())).thenReturn(parse("""
                { "prices": [
                  { "stockCode": "005930", "rows": [ { "tradeDate": "20261016", "openPrice": 100, "closePrice": 105 } ] },
                  { "stockCode": "000660", "rows": [ { "tradeDate": "20261016", "openPrice": 200, "closePrice": 210 } ] } ],
                  "errors": [ { "stockCode": "000660", "reason": "조회 실패" } ] }
                """));
        saveReturnsRowCount();

        BackfillResult result = useCase.backfill(plan("005930", "000660"), null);

        assertThat(allSavedRows()).extracting(DailyPriceRow::stockCode).containsOnly("005930");
        assertThat(result.failedStockCodes()).containsExactly("000660");
    }

    /** errors와 skipped에 함께 오면 건너뜀이 아니라 실패다. 실패를 숨기면 다시 받지 않는다. */
    @Test
    void errors와_skipped에_함께_온_종목은_실패() {
        Mockito.when(krxService.getDailyPricesByRange(any(), any(), anyList())).thenReturn(parse("""
                { "prices": [ { "stockCode": "005930", "rows": [ { "tradeDate": "20261016", "openPrice": 100, "closePrice": 105 } ] } ],
                  "skipped": [ { "stockCode": "000660", "reason": "해당 기간 일봉 없음" } ],
                  "errors": [ { "stockCode": "000660", "reason": "조회 실패" } ] }
                """));
        saveReturnsRowCount();

        BackfillResult result = useCase.backfill(plan("005930", "000660"), null);

        assertThat(result.failedStockCodes()).containsExactly("000660");
        assertThat(result.skippedStockCodes()).isEmpty();
    }

    /** 작업이 예외로 끝나도 잠금은 풀려야 한다. 안 풀리면 임대 시간 동안 일일 적재도 막힌다. */
    @Test
    void 비동기_백필이_끝나면_잠금을_푼다() {
        Mockito.when(krxService.getDailyPricesByRange(any(), any(), anyList())).thenThrow(new RuntimeException("boom"));

        useCase.backfillAsync(plan("005930"), "token");

        verify(guard).release("token");
    }

    /** 긴 백필이 임대 시간을 넘겨 다른 서버와 겹치지 않도록 묶음마다 임대를 갱신한다. 마지막 묶음 뒤에는 갱신할 필요가 없다. */
    @Test
    void 묶음마다_잠금_임대를_갱신한다() {
        krxEchoesRequestedCodes();
        saveReturnsRowCount();
        Mockito.when(guard.renew("token")).thenReturn(true);

        useCase.backfill(new BackfillPlan(THREE_YEARS_AGO, LATEST, codes(45)), "token");

        verify(guard, times(2)).renew("token");
    }

    /** 잠금을 잃었으면 다른 실행이 돌고 있다. 남은 묶음은 부르지 않고 실패로 남긴다. */
    @Test
    void 잠금을_잃으면_남은_묶음은_부르지_않고_실패로_남긴다() {
        List<String> all = codes(45);
        krxEchoesRequestedCodes();
        saveReturnsRowCount();
        Mockito.when(guard.renew("token")).thenReturn(false);

        BackfillResult result = useCase.backfill(new BackfillPlan(THREE_YEARS_AGO, LATEST, all), "token");

        verify(krxService, times(1)).getDailyPricesByRange(any(), any(), anyList());
        assertThat(result.upsertedRows()).isEqualTo(20);
        assertThat(result.failedStockCodes()).containsExactlyElementsOf(all.subList(20, 45));
    }
}
