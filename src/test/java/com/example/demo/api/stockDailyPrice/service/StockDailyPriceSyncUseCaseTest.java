package com.example.demo.api.stockDailyPrice.service;

import com.example.demo.api.krx.dto.KrxDailyPricesByDateResponseDto;
import com.example.demo.api.krx.service.KrxService;
import com.example.demo.api.stockDailyPrice.service.StockDailyPriceSyncUseCase.DailyPlan;
import com.example.demo.api.stockDailyPrice.service.StockDailyPriceSyncUseCase.DailyTarget;
import com.example.demo.api.stockDailyPrice.service.StockDailyPriceSyncUseCase.DateResult;
import com.example.demo.common.exception.BaseErrorCode;
import com.example.demo.common.exception.GeneralException;
import com.example.demo.domain.stock.service.StockQueryService;
import com.example.demo.domain.stockDailyPrice.entity.DailyPriceRow;
import com.example.demo.domain.stockDailyPrice.entity.DailyPriceSyncResult;
import com.example.demo.domain.stockDailyPrice.entity.DailyPriceSyncStatus;
import com.example.demo.domain.stockDailyPrice.entity.StockDailyPriceSyncLog;
import com.example.demo.domain.stockDailyPrice.exception.StockDailyPriceErrorStatus;
import com.example.demo.domain.stockDailyPrice.service.StockDailyPriceCommandService;
import com.example.demo.domain.stockDailyPrice.service.StockDailyPriceQueryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static com.example.demo.common.consts.StaticVariable.DAILY_PRICE_SYNC_LOOKBACK_DAYS;
import static com.example.demo.common.consts.StaticVariable.DAILY_PRICE_SYNC_MAX_ATTEMPT;

/** 일일 적재: 이력 기반 계획(새 날짜, PARTIAL·FAILED 재시도, 상한), 날짜별 상태 판정. */
class StockDailyPriceSyncUseCaseTest {

    // 2026-10-12(월) ~ 10-16(금), 10-17·18 주말, 10-19(월)
    private static final LocalDate MON = LocalDate.of(2026, 10, 12);
    private static final LocalDate FRI = LocalDate.of(2026, 10, 16);
    private static final LocalDate NEXT_MON = LocalDate.of(2026, 10, 19);
    private static final List<String> ALL = List.of("005930", "000660", "035420");

    private final KrxService krxService = Mockito.mock(KrxService.class);
    private final StockQueryService stockQueryService = Mockito.mock(StockQueryService.class);
    private final StockDailyPriceQueryService queryService = Mockito.mock(StockDailyPriceQueryService.class);
    private final StockDailyPriceCommandService commandService = Mockito.mock(StockDailyPriceCommandService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final DailyPriceJobGuard guard = Mockito.mock(DailyPriceJobGuard.class);

    private final StockDailyPriceSyncUseCase useCase = new StockDailyPriceSyncUseCase(
            krxService, stockQueryService, queryService, commandService, new DailyPriceIngestor(commandService), guard);

    @BeforeEach
    void setUp() {
        Mockito.when(stockQueryService.getAllStockCodes()).thenReturn(ALL);
        Mockito.when(queryService.getRetryableSyncLogs(anyInt())).thenReturn(List.of());
        Mockito.when(queryService.getSyncLogs(any(), any())).thenReturn(Map.of());
        Mockito.when(queryService.getLatestFinishedSyncDate()).thenReturn(Optional.empty());
        Mockito.when(queryService.getLatestDailyPriceDateOverall()).thenReturn(Optional.empty());
        Mockito.when(queryService.getEarliestSyncDate()).thenReturn(Optional.empty());
        Mockito.when(commandService.saveDailyPrices(anyList()))
                .thenAnswer(invocation -> ((List<?>) invocation.getArgument(0)).size());
    }

    private void latestTradeDate(LocalDate date) {
        Mockito.when(stockQueryService.getLatestTradeDate()).thenReturn(Optional.of(date));
    }

    private static StockDailyPriceSyncLog log(LocalDate date, DailyPriceSyncStatus status, int attempts, List<String> failed) {
        StockDailyPriceSyncLog log = null;
        for (int i = 0; i < attempts; i++) {
            DailyPriceSyncResult result = new DailyPriceSyncResult(status, 3, 0, 0, failed,
                    LocalDateTime.now(), LocalDateTime.now(), null);
            if (log == null) {
                log = StockDailyPriceSyncLog.first(date, result);
            } else {
                log.apply(result);
            }
        }
        return log;
    }

    private KrxDailyPricesByDateResponseDto parse(String json) {
        try {
            return objectMapper.readValue(json, KrxDailyPricesByDateResponseDto.class);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String price(String code) {
        return "{ \"stockCode\": \"%s\", \"openPrice\": 100, \"highPrice\": 110, \"lowPrice\": 90, \"closePrice\": 105 }"
                .formatted(code);
    }

    private DailyPriceSyncResult recorded(LocalDate date) {
        ArgumentCaptor<DailyPriceSyncResult> captor = ArgumentCaptor.forClass(DailyPriceSyncResult.class);
        verify(commandService).recordSyncResult(eq(date), captor.capture());
        return captor.getValue();
    }

    private void assertErrorCode(Runnable call, BaseErrorCode expected) {
        assertThatThrownBy(call::run)
                .isInstanceOf(GeneralException.class)
                .extracting(e -> ((GeneralException) e).getErrorReasonHttpStatus().getCode())
                .isEqualTo(expected.getReasonHttpStatus().getCode());
    }

    // ---------- 계획 ----------

    /** from~to의 평일마다 SUCCESS 이력. overrides로 특정 날짜의 이력을 바꾸거나(null이면 빼거나) 한다. */
    private Map<LocalDate, StockDailyPriceSyncLog> successLogs(LocalDate from, LocalDate to,
                                                               Map<LocalDate, StockDailyPriceSyncLog> overrides) {
        Map<LocalDate, StockDailyPriceSyncLog> logs = new java.util.TreeMap<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            if (d.getDayOfWeek().getValue() <= 5) {
                logs.put(d, log(d, DailyPriceSyncStatus.SUCCESS, 1, List.of()));
            }
        }
        overrides.forEach((date, log) -> {
            if (log == null) {
                logs.remove(date);
            } else {
                logs.put(date, log);
            }
        });
        return logs;
    }

    /** 이력 조회를 실제 쿼리처럼 기간으로 걸러 돌려준다. */
    private void givenLogs(Map<LocalDate, StockDailyPriceSyncLog> logs) {
        Mockito.when(queryService.getEarliestSyncDate())
                .thenReturn(logs.keySet().stream().min(LocalDate::compareTo));
        Mockito.when(queryService.getLatestFinishedSyncDate()).thenReturn(logs.values().stream()
                .filter(l -> l.getStatus().isFinished())
                .map(StockDailyPriceSyncLog::getTradeDate)
                .max(LocalDate::compareTo));
        Mockito.when(queryService.getSyncLogs(any(), any())).thenAnswer(invocation -> {
            LocalDate from = invocation.getArgument(0);
            LocalDate to = invocation.getArgument(1);
            Map<LocalDate, StockDailyPriceSyncLog> inRange = new java.util.TreeMap<>();
            logs.forEach((d, l) -> {
                if (!d.isBefore(from) && !d.isAfter(to)) {
                    inRange.put(d, l);
                }
            });
            return inRange;
        });
    }

    private static final LocalDate SEP_1 = LocalDate.of(2026, 9, 1);

    /** 평소 모습. 어제까지 끝났으면 오늘 몫 하루만 받는다. */
    @Test
    void 어제까지_끝났으면_최근_거래일_하루만() {
        latestTradeDate(FRI);
        givenLogs(successLogs(SEP_1, FRI.minusDays(1), Map.of()));

        DailyPlan plan = useCase.plan(null);

        assertThat(plan.targets()).singleElement().satisfies(t -> {
            assertThat(t.tradeDate()).isEqualTo(FRI);
            assertThat(t.stockCodes()).containsExactlyElementsOf(ALL);
        });
    }

    /** 며칠 실패했거나 실행기가 거부한 날은 이력이 없다. 다음 실행이 함께 받는다. 주말은 뺀다. */
    @Test
    void 이력이_없는_평일을_모두_받고_주말은_뺀다() {
        latestTradeDate(NEXT_MON);
        givenLogs(successLogs(SEP_1, LocalDate.of(2026, 10, 14), Map.of()));

        assertThat(useCase.plan(null).tradeDates()).containsExactly(LocalDate.of(2026, 10, 15), FRI, NEXT_MON);
    }

    /**
     * 데이터는 저장됐지만 이력 기록에 실패한 날, 또는 date를 지정해 뒤 날짜를 먼저 성공 처리해 생긴 앞쪽 빈 날.
     * 끝난 마지막 이력 다음 날만 보면 영영 다시 찾지 못한다.
     */
    @Test
    void 끝난_마지막_이력보다_앞의_빈_날도_30일_안이면_다시_찾는다() {
        latestTradeDate(FRI);
        java.util.Map<LocalDate, StockDailyPriceSyncLog> gap = new java.util.HashMap<>();
        gap.put(LocalDate.of(2026, 10, 13), null);
        givenLogs(successLogs(SEP_1, FRI, gap));

        assertThat(useCase.plan(null).tradeDates()).containsExactly(LocalDate.of(2026, 10, 13));
    }

    /** 오래된 구간을 정리 목적으로 지웠을 때 다시 받아오지 않는다. */
    @Test
    void 삼십일보다_오래된_빈_날은_찾지_않는다() {
        latestTradeDate(FRI);
        java.util.Map<LocalDate, StockDailyPriceSyncLog> gap = new java.util.HashMap<>();
        gap.put(LocalDate.of(2026, 9, 10), null);
        givenLogs(successLogs(LocalDate.of(2026, 8, 3), FRI, gap));

        assertThat(useCase.plan(null).isEmpty()).isTrue();
        verify(queryService).getSyncLogs(FRI.minusDays(DAILY_PRICE_SYNC_LOOKBACK_DAYS), FRI);
    }

    /** 장애가 30일보다 길면 끝난 마지막 이력 다음 날부터 찾는다. 30일 앞만 보면 그 사이가 빠진다. */
    @Test
    void 장애가_30일보다_길면_끝난_다음_날부터() {
        latestTradeDate(FRI);
        givenLogs(successLogs(LocalDate.of(2026, 8, 3), SEP_1, Map.of()));

        List<LocalDate> dates = useCase.plan(null).tradeDates();

        assertThat(dates).first().isEqualTo(LocalDate.of(2026, 9, 2));
        assertThat(dates).last().isEqualTo(FRI);
    }

    /**
     * 최근 10일 중 앞 3일을 전 종목 삭제한 경우. 그 3일의 이력도 지워져 남은 첫 이력이 뒤로 밀린다.
     * 첫 이력을 하한으로 삼으면 지운 3일을 다시 찾지 못한다. 30일 안이면 첫 이력과 상관없이 찾는다.
     */
    @Test
    void 앞쪽_이력을_지워도_30일_안이면_다시_찾는다() {
        latestTradeDate(FRI);
        givenLogs(successLogs(LocalDate.of(2026, 10, 8), FRI, Map.of()));

        assertThat(useCase.plan(null).tradeDates()).contains(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 6),
                LocalDate.of(2026, 10, 7));
        verify(queryService).getSyncLogs(FRI.minusDays(DAILY_PRICE_SYNC_LOOKBACK_DAYS), FRI);
    }

    /** 백필 직후 첫 실행. 이력이 없으니 일봉 테이블의 마지막 날짜 다음 날부터 이어 받는다. */
    @Test
    void 이력이_없으면_일봉_마지막_날짜_다음_날부터() {
        latestTradeDate(NEXT_MON);
        Mockito.when(queryService.getLatestDailyPriceDateOverall()).thenReturn(Optional.of(FRI));

        assertThat(useCase.plan(null).tradeDates()).containsExactly(NEXT_MON);
    }

    /** 과거는 /backfill이 맡는다. 데이터가 하나도 없으면 최근 거래일 하루만 받는다. */
    @Test
    void 이력도_일봉도_없으면_최근_거래일_하루만() {
        latestTradeDate(FRI);

        assertThat(useCase.plan(null).tradeDates()).containsExactly(FRI);
    }

    @Test
    void 이미_최신이면_빈_계획() {
        latestTradeDate(FRI);
        givenLogs(successLogs(SEP_1, FRI, Map.of()));

        assertThat(useCase.plan(null).isEmpty()).isTrue();
    }

    /** 일일 배치가 늦어 최근 거래일이 갱신되지 않은 경우. 오류가 아니라 할 일이 없을 뿐이다. */
    @Test
    void 최근_거래일이_갱신되지_않았으면_할_일이_없다() {
        latestTradeDate(FRI.minusDays(1));
        givenLogs(successLogs(SEP_1, FRI.minusDays(1), Map.of()));

        assertThat(useCase.plan(null).isEmpty()).isTrue();
    }

    // ---------- 계획: 재시도 ----------

    /** 199종목이 저장된 날도 다시 요청할 수 있어야 한다. 실패 종목만 요청한다. */
    @Test
    void PARTIAL_날짜는_실패_종목만_다시_요청한다() {
        latestTradeDate(FRI);
        StockDailyPriceSyncLog partial = log(MON, DailyPriceSyncStatus.PARTIAL, 1, List.of("000660"));
        givenLogs(successLogs(SEP_1, FRI, Map.of(MON, partial)));
        Mockito.when(queryService.getRetryableSyncLogs(DAILY_PRICE_SYNC_MAX_ATTEMPT)).thenReturn(List.of(partial));

        assertThat(useCase.plan(null).targets()).singleElement().satisfies(t -> {
            assertThat(t.tradeDate()).isEqualTo(MON);
            assertThat(t.stockCodes()).containsExactly("000660");
        });
    }

    @Test
    void 재시도와_새_날짜를_함께_날짜순으로() {
        latestTradeDate(FRI);
        StockDailyPriceSyncLog failed = log(MON, DailyPriceSyncStatus.FAILED, 2, ALL);
        givenLogs(successLogs(SEP_1, FRI.minusDays(1), Map.of(MON, failed)));
        Mockito.when(queryService.getRetryableSyncLogs(DAILY_PRICE_SYNC_MAX_ATTEMPT)).thenReturn(List.of(failed));

        assertThat(useCase.plan(null).tradeDates()).containsExactly(MON, FRI);
    }

    /** 상한을 채운 날짜는 재시도 조회에서 빠지고, 이력이 있으니 빈 날로도 잡히지 않는다. */
    @Test
    void 상한을_채운_날짜는_다시_받지_않는다() {
        latestTradeDate(FRI);
        givenLogs(successLogs(SEP_1, FRI, Map.of(FRI, log(FRI, DailyPriceSyncStatus.PARTIAL, 5, List.of("000660")))));

        assertThat(useCase.plan(null).isEmpty()).isTrue();
        verify(queryService).getRetryableSyncLogs(DAILY_PRICE_SYNC_MAX_ATTEMPT);
    }

    // ---------- 계획: date 지정 ----------

    @Test
    void date를_지정하면_그_날짜만_전_종목() {
        latestTradeDate(FRI);

        DailyPlan plan = useCase.plan(MON);

        assertThat(plan.targets()).containsExactly(new DailyTarget(MON, ALL));
        verify(queryService, never()).getRetryableSyncLogs(anyInt());
    }

    @Test
    void date가_최근_거래일보다_뒤면_4255() {
        latestTradeDate(FRI);

        assertErrorCode(() -> useCase.plan(NEXT_MON), StockDailyPriceErrorStatus.DAILY_PRICE_INVALID_RANGE);
    }

    @Test
    void 반영된_시세가_없으면_4256() {
        Mockito.when(stockQueryService.getLatestTradeDate()).thenReturn(Optional.empty());

        assertErrorCode(() -> useCase.plan(null), StockDailyPriceErrorStatus.DAILY_PRICE_NOT_SYNCED_YET);
    }

    // ---------- 실행: 상태 판정 ----------

    private DailyPlan planOf(LocalDate date, List<String> codes) {
        return new DailyPlan(date, List.of(new DailyTarget(date, codes)));
    }

    @Test
    void 전부_받으면_SUCCESS() {
        Mockito.when(krxService.getDailyPricesByDate(any(), anyList())).thenReturn(parse(
                "{ \"date\": \"20261016\", \"prices\": [" + price("005930") + "," + price("000660") + "," + price("035420") + "] }"));

        List<DateResult> results = useCase.run(planOf(FRI, ALL), null);

        assertThat(results).singleElement().extracting(DateResult::status).isEqualTo(DailyPriceSyncStatus.SUCCESS);
        DailyPriceSyncResult recorded = recorded(FRI);
        assertThat(recorded.upsertedRows()).isEqualTo(3);
        assertThat(recorded.failedStockCodes()).isEmpty();
    }

    /** 날짜 모드 응답에는 거래일이 없다. 요청한 날짜로 저장되어야 한다. */
    @Test
    @SuppressWarnings("unchecked")
    void 요청한_날짜로_저장한다() {
        Mockito.when(krxService.getDailyPricesByDate(any(), anyList())).thenReturn(parse(
                "{ \"prices\": [" + price("005930") + "] }"));

        useCase.run(planOf(FRI, List.of("005930")), null);

        ArgumentCaptor<List<DailyPriceRow>> captor = ArgumentCaptor.forClass(List.class);
        verify(commandService).saveDailyPrices(captor.capture());
        assertThat(captor.getValue()).singleElement().extracting(DailyPriceRow::tradeDate).isEqualTo(FRI);
    }

    /** 거래정지 종목은 매일 실패로 남으면 안 된다. */
    @Test
    void skipped만_남으면_SUCCESS() {
        Mockito.when(krxService.getDailyPricesByDate(any(), anyList())).thenReturn(parse("""
                { "prices": [ %s, %s ],
                  "skipped": [ { "stockCode": "035420", "reason": "거래정지" } ] }
                """.formatted(price("005930"), price("000660"))));

        useCase.run(planOf(FRI, ALL), null);

        DailyPriceSyncResult recorded = recorded(FRI);
        assertThat(recorded.status()).isEqualTo(DailyPriceSyncStatus.SUCCESS);
        assertThat(recorded.skippedStockCount()).isEqualTo(1);
    }

    @Test
    void 일부_종목이_빠지면_PARTIAL과_실패_종목() {
        Mockito.when(krxService.getDailyPricesByDate(any(), anyList())).thenReturn(parse(
                "{ \"prices\": [" + price("005930") + "] }"));

        useCase.run(planOf(FRI, ALL), null);

        DailyPriceSyncResult recorded = recorded(FRI);
        assertThat(recorded.status()).isEqualTo(DailyPriceSyncStatus.PARTIAL);
        assertThat(recorded.failedStockCodes()).containsExactly("000660", "035420");
    }

    @Test
    void 가격이_무효인_종목은_실패_종목() {
        Mockito.when(krxService.getDailyPricesByDate(any(), anyList())).thenReturn(parse("""
                { "prices": [ %s,
                  { "stockCode": "000660", "openPrice": 0, "highPrice": 0, "lowPrice": 0, "closePrice": 100 } ] }
                """.formatted(price("005930"))));

        useCase.run(planOf(FRI, List.of("005930", "000660")), null);

        assertThat(recorded(FRI).failedStockCodes()).containsExactly("000660");
    }

    /** 가격과 건너뜀에 함께 온 종목은 어느 쪽이 맞는지 알 수 없다. 저장하지 않고 실패로 남겨 다시 받는다. */
    @Test
    @SuppressWarnings("unchecked")
    void 가격과_건너뜀에_함께_온_종목은_저장하지_않고_실패() {
        Mockito.when(krxService.getDailyPricesByDate(any(), anyList())).thenReturn(parse("""
                { "prices": [ %s, %s ],
                  "skipped": [ { "stockCode": "000660", "reason": "거래정지" } ] }
                """.formatted(price("005930"), price("000660"))));

        useCase.run(planOf(FRI, List.of("005930", "000660")), null);

        DailyPriceSyncResult recorded = recorded(FRI);
        assertThat(recorded.status()).isEqualTo(DailyPriceSyncStatus.PARTIAL);
        assertThat(recorded.failedStockCodes()).containsExactly("000660");
        assertThat(recorded.skippedStockCount()).isZero();
        ArgumentCaptor<List<DailyPriceRow>> captor = ArgumentCaptor.forClass(List.class);
        verify(commandService).saveDailyPrices(captor.capture());
        assertThat(captor.getValue()).extracting(DailyPriceRow::stockCode).containsExactly("005930");
    }

    @Test
    void 받은_것도_건너뛴_것도_없으면_FAILED() {
        Mockito.when(krxService.getDailyPricesByDate(any(), anyList())).thenReturn(parse("{ \"prices\": [] }"));

        useCase.run(planOf(FRI, ALL), null);

        DailyPriceSyncResult recorded = recorded(FRI);
        assertThat(recorded.status()).isEqualTo(DailyPriceSyncStatus.FAILED);
        assertThat(recorded.failedStockCodes()).containsExactlyElementsOf(ALL);
    }

    @Test
    void 휴장일은_HOLIDAY이고_저장하지_않는다() {
        Mockito.when(krxService.getDailyPricesByDate(any(), anyList()))
                .thenReturn(parse("{ \"marketClosed\": true, \"prices\": [], \"skipped\": [] }"));

        useCase.run(planOf(FRI, ALL), null);

        assertThat(recorded(FRI).status()).isEqualTo(DailyPriceSyncStatus.HOLIDAY);
        verify(commandService, never()).saveDailyPrices(anyList());
    }

    /** 한 날짜의 호출 실패가 다음 날짜를 막지 않는다. 실패 날짜는 요청 종목 전체를 실패로 남긴다. */
    @Test
    void 호출이_실패한_날짜만_FAILED이고_다음_날짜는_계속() {
        Mockito.when(krxService.getDailyPricesByDate(eq(MON), anyList())).thenThrow(new RuntimeException("Lambda timeout"));
        Mockito.when(krxService.getDailyPricesByDate(eq(FRI), anyList())).thenReturn(parse(
                "{ \"prices\": [" + price("005930") + "] }"));

        List<DateResult> results = useCase.run(new DailyPlan(FRI, List.of(
                new DailyTarget(MON, List.of("005930")), new DailyTarget(FRI, List.of("005930")))), null);

        assertThat(results).extracting(DateResult::status)
                .containsExactly(DailyPriceSyncStatus.FAILED, DailyPriceSyncStatus.SUCCESS);
        assertThat(recorded(MON).failedStockCodes()).containsExactly("005930");
    }

    /** 이력 기록이 실패해도 다음 날짜는 처리한다. 기록이 없으면 다음 실행이 그 날짜를 다시 받는다. */
    @Test
    void 이력_기록이_실패해도_다음_날짜는_계속() {
        Mockito.when(krxService.getDailyPricesByDate(any(), anyList())).thenReturn(parse(
                "{ \"prices\": [" + price("005930") + "] }"));
        Mockito.doThrow(new RuntimeException("db down")).when(commandService).recordSyncResult(eq(MON), any());

        List<DateResult> results = useCase.run(new DailyPlan(FRI, List.of(
                new DailyTarget(MON, List.of("005930")), new DailyTarget(FRI, List.of("005930")))), null);

        assertThat(results).hasSize(2);
        verify(commandService).recordSyncResult(eq(FRI), any());
    }

    /** 같은 날짜 이력을 다른 실행이 먼저 만들었으면 한 번 다시 기록한다. 두 번째에는 그 행을 찾아 덮어쓴다. */
    @Test
    void 이력_생성_충돌은_한_번_다시_기록한다() {
        Mockito.when(krxService.getDailyPricesByDate(any(), anyList())).thenReturn(parse(
                "{ \"prices\": [" + price("005930") + "] }"));
        Mockito.doThrow(new org.springframework.dao.DataIntegrityViolationException("uk_stock_daily_price_sync_log_trade_date"))
                .doNothing()
                .when(commandService).recordSyncResult(eq(FRI), any());

        useCase.run(planOf(FRI, List.of("005930")), null);

        verify(commandService, Mockito.times(2)).recordSyncResult(eq(FRI), any());
    }

    @Test
    void 비동기_일일_적재가_끝나면_잠금을_푼다() {
        useCase.runAsync(new DailyPlan(FRI, List.of()), "token");

        verify(guard).release("token");
    }

    /** 잠금을 잃었으면 남은 날짜는 처리하지 않는다. 이력이 남지 않아 다음 실행이 다시 찾는다. */
    @Test
    void 잠금을_잃으면_남은_날짜는_처리하지_않는다() {
        Mockito.when(krxService.getDailyPricesByDate(any(), anyList())).thenReturn(parse(
                "{ \"prices\": [" + price("005930") + "] }"));
        Mockito.when(guard.renew("token")).thenReturn(false);

        List<DateResult> results = useCase.run(new DailyPlan(FRI, List.of(
                new DailyTarget(MON, List.of("005930")), new DailyTarget(FRI, List.of("005930")))), "token");

        assertThat(results).extracting(DateResult::tradeDate).containsExactly(MON);
        verify(krxService, never()).getDailyPricesByDate(eq(FRI), anyList());
    }

    @Test
    void 날짜마다_잠금_임대를_갱신한다() {
        Mockito.when(krxService.getDailyPricesByDate(any(), anyList())).thenReturn(parse(
                "{ \"prices\": [" + price("005930") + "] }"));
        Mockito.when(guard.renew("token")).thenReturn(true);

        useCase.run(new DailyPlan(FRI, List.of(
                new DailyTarget(MON, List.of("005930")), new DailyTarget(FRI, List.of("005930")))), "token");

        verify(guard, Mockito.times(1)).renew("token");
    }
}
