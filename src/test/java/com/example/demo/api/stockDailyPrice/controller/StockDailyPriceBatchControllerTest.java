package com.example.demo.api.stockDailyPrice.controller;

import com.example.demo.api.common.dto.ApiResponseDto;
import com.example.demo.api.stockDailyPrice.dto.StockDailyPriceResponseDto.BackfillTriggerResponse;
import com.example.demo.api.stockDailyPrice.dto.StockDailyPriceResponseDto.DailyTriggerResponse;
import com.example.demo.api.stockDailyPrice.dto.StockDailyPriceResponseDto.DeleteResponse;
import com.example.demo.api.stockDailyPrice.service.StockDailyPriceBackfillUseCase.BackfillPlan;
import com.example.demo.api.stockDailyPrice.service.StockDailyPriceBackfillUseCase;
import com.example.demo.api.stockDailyPrice.service.DailyPriceJobGuard;
import com.example.demo.api.stockDailyPrice.service.StockDailyPriceDeleteUseCase;
import com.example.demo.api.stockDailyPrice.service.StockDailyPriceSyncUseCase.DailyPlan;
import com.example.demo.api.stockDailyPrice.service.StockDailyPriceSyncUseCase.DailyTarget;
import com.example.demo.api.stockDailyPrice.service.StockDailyPriceSyncUseCase;
import com.example.demo.common.exception.GeneralException;
import com.example.demo.domain.batch.exception.BatchErrorStatus;
import com.example.demo.domain.stockDailyPrice.entity.StockDailyPriceDeleteResult;
import com.example.demo.domain.stockDailyPrice.exception.StockDailyPriceErrorStatus;
import com.example.demo.domain.stockDailyPrice.exception.StockDailyPriceHandler;
import com.example.demo.security.batch.BatchSecretVerifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/** 스프링 컨텍스트 없이 컨트롤러만 검증한다. BatchControllerTest와 같은 방식이다. */
class StockDailyPriceBatchControllerTest {

    private static final String SECRET = "test-batch-secret";
    private static final LocalDate LATEST = LocalDate.of(2026, 10, 16);
    private static final LocalDate FROM = LocalDate.of(2023, 10, 16);
    private static final BackfillPlan BACKFILL = new BackfillPlan(FROM, LATEST, List.of("005930", "000660", "035420"));
    private static final DailyPlan DAILY = new DailyPlan(LATEST, List.of(
            new DailyTarget(LocalDate.of(2026, 10, 12), List.of("000660")),
            new DailyTarget(LATEST, List.of("005930", "000660"))));

    private final StockDailyPriceBackfillUseCase backfillUseCase = Mockito.mock(StockDailyPriceBackfillUseCase.class);
    private final StockDailyPriceSyncUseCase dailyUseCase = Mockito.mock(StockDailyPriceSyncUseCase.class);
    private final StockDailyPriceDeleteUseCase deleteUseCase = Mockito.mock(StockDailyPriceDeleteUseCase.class);
    private final DailyPriceJobGuard guard = Mockito.mock(DailyPriceJobGuard.class);

    private final StockDailyPriceBatchController controller = new StockDailyPriceBatchController(
            backfillUseCase, dailyUseCase, deleteUseCase, guard, new BatchSecretVerifier(SECRET));

    private static final String TOKEN = "lock-token";

    @BeforeEach
    void setUp() {
        Mockito.when(guard.tryAcquire()).thenReturn(Optional.of(TOKEN));
    }

    private static int code(Throwable e) {
        return ((GeneralException) e).getErrorReasonHttpStatus().getCode();
    }

    // ---------- 시크릿 ----------

    /** /api/v1/batch/**는 permitAll이라 세 엔드포인트 모두 직접 검증해야 한다. */
    @Test
    void 세_엔드포인트_모두_시크릿이_없으면_거부한다() {
        assertThatThrownBy(() -> controller.triggerBackfill(null, null, null, null))
                .satisfies(e -> assertThat(code(e)).isEqualTo(BatchErrorStatus.BATCH_TRIGGER_UNAUTHORIZED.getCode()));
        assertThatThrownBy(() -> controller.triggerDaily("wrong", null))
                .satisfies(e -> assertThat(code(e)).isEqualTo(BatchErrorStatus.BATCH_TRIGGER_UNAUTHORIZED.getCode()));
        assertThatThrownBy(() -> controller.deleteDailyPrices(null, FROM, LATEST, null))
                .satisfies(e -> assertThat(code(e)).isEqualTo(BatchErrorStatus.BATCH_TRIGGER_UNAUTHORIZED.getCode()));

        verify(backfillUseCase, never()).plan(any(), any(), any());
        verify(dailyUseCase, never()).plan(any());
        verify(deleteUseCase, never()).delete(any(), any(), any());
    }

    // ---------- /backfill ----------

    @Test
    void 백필은_대상을_확정한_뒤_202로_접수하고_확정된_대상을_돌려준다() {
        Mockito.when(backfillUseCase.plan(null, null, null)).thenReturn(BACKFILL);

        ResponseEntity<ApiResponseDto<BackfillTriggerResponse>> response = controller.triggerBackfill(SECRET, null, null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody().getResult()).satisfies(r -> {
            assertThat(r.isAccepted()).isTrue();
            assertThat(r.getFrom()).isEqualTo(FROM);
            assertThat(r.getTo()).isEqualTo(LATEST);
            assertThat(r.getTargetStockCount()).isEqualTo(3);
        });
        verify(backfillUseCase).backfillAsync(BACKFILL, TOKEN);
    }

    /** 비동기 안에서 검증하면 잘못된 요청이 202로 접수되고 오류는 로그에만 남는다. */
    @Test
    void 백필_검증_오류는_비동기로_넘기기_전에_거절한다() {
        Mockito.when(backfillUseCase.plan(any(), any(), any())).thenThrow(StockDailyPriceHandler.unknownStockCodes());

        assertThatThrownBy(() -> controller.triggerBackfill(SECRET, List.of("999999"), null, null))
                .satisfies(e -> assertThat(code(e)).isEqualTo(StockDailyPriceErrorStatus.DAILY_PRICE_UNKNOWN_STOCK_CODES.getCode()));
        verify(backfillUseCase, never()).backfillAsync(any(), any());
    }

    /** 실행기가 거부해 작업이 시작되지 못하면 잡은 잠금을 바로 푼다. 안 풀면 임대 시간 동안 아무것도 못 돈다. */
    @Test
    void 실행기가_거부하면_백필은_accepted_false이고_잠금을_푼다() {
        Mockito.when(backfillUseCase.plan(null, null, null)).thenReturn(BACKFILL);
        doThrow(new TaskRejectedException("busy")).when(backfillUseCase).backfillAsync(BACKFILL, TOKEN);

        ResponseEntity<ApiResponseDto<BackfillTriggerResponse>> response = controller.triggerBackfill(SECRET, null, null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody().getResult().isAccepted()).isFalse();
        assertThat(response.getBody().getResult().getMessage()).contains("실행 중");
        verify(guard).release(TOKEN);
    }

    /** 다른 서버가 일봉 적재 중이면 잠금을 못 잡는다. 실행기에 넘기지 않는다. */
    @Test
    void 다른_서버가_잠금을_갖고_있으면_백필은_accepted_false() {
        Mockito.when(backfillUseCase.plan(null, null, null)).thenReturn(BACKFILL);
        Mockito.when(guard.tryAcquire()).thenReturn(Optional.empty());

        ResponseEntity<ApiResponseDto<BackfillTriggerResponse>> response = controller.triggerBackfill(SECRET, null, null, null);

        assertThat(response.getBody().getResult().isAccepted()).isFalse();
        verify(backfillUseCase, never()).backfillAsync(any(), any());
    }

    // ---------- /daily ----------

    @Test
    void 일일_적재는_받을_날짜를_돌려주고_202로_접수한다() {
        Mockito.when(dailyUseCase.plan(null)).thenReturn(DAILY);

        ResponseEntity<ApiResponseDto<DailyTriggerResponse>> response = controller.triggerDaily(SECRET, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody().getResult()).satisfies(r -> {
            assertThat(r.isAccepted()).isTrue();
            assertThat(r.getLatestTradeDate()).isEqualTo(LATEST);
            assertThat(r.getTradeDates()).containsExactly(LocalDate.of(2026, 10, 12), LATEST);
        });
        verify(dailyUseCase).runAsync(DAILY, TOKEN);
    }

    /** 스케줄러가 매일 부르므로 할 일이 없는 것을 실패로 보이면 안 된다. 실행기에도 넘기지 않는다. */
    @Test
    void 이미_최신이면_실행하지_않고_accepted_true() {
        DailyPlan empty = new DailyPlan(LATEST, List.of());
        Mockito.when(dailyUseCase.plan(null)).thenReturn(empty);

        ResponseEntity<ApiResponseDto<DailyTriggerResponse>> response = controller.triggerDaily(SECRET, null);

        assertThat(response.getBody().getResult()).satisfies(r -> {
            assertThat(r.isAccepted()).isTrue();
            assertThat(r.getTradeDates()).isEmpty();
            assertThat(r.getMessage()).contains("이미 최신");
        });
        verify(dailyUseCase, never()).runAsync(any(), any());
        verify(guard, never()).tryAcquire();
    }

    /** 백필이 어느 서버에서든 돌고 있으면 잠금을 못 잡는다. 이력이 남지 않으니 다음 호출이 따라잡는다. */
    @Test
    void 백필이_돌고_있으면_일일_적재는_accepted_false() {
        Mockito.when(dailyUseCase.plan(null)).thenReturn(DAILY);
        Mockito.when(guard.tryAcquire()).thenReturn(Optional.empty());

        ResponseEntity<ApiResponseDto<DailyTriggerResponse>> response = controller.triggerDaily(SECRET, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody().getResult().isAccepted()).isFalse();
        verify(dailyUseCase, never()).runAsync(any(), any());
    }

    @Test
    void 실행기가_거부하면_일일_적재도_잠금을_푼다() {
        Mockito.when(dailyUseCase.plan(null)).thenReturn(DAILY);
        doThrow(new TaskRejectedException("busy")).when(dailyUseCase).runAsync(DAILY, TOKEN);

        ResponseEntity<ApiResponseDto<DailyTriggerResponse>> response = controller.triggerDaily(SECRET, null);

        assertThat(response.getBody().getResult().isAccepted()).isFalse();
        verify(guard).release(TOKEN);
    }

    @Test
    void date를_그대로_넘긴다() {
        LocalDate date = LocalDate.of(2026, 10, 14);
        Mockito.when(dailyUseCase.plan(date)).thenReturn(new DailyPlan(LATEST, List.of(new DailyTarget(date, List.of("005930")))));

        controller.triggerDaily(SECRET, date);

        verify(dailyUseCase).plan(date);
    }

    // ---------- DELETE ----------

    @Test
    void 삭제는_지운_수와_다시_채우는_방법을_돌려준다() {
        Mockito.when(deleteUseCase.delete(FROM, LATEST, null)).thenReturn(new StockDailyPriceDeleteResult(1200, 5));

        DeleteResponse response = controller.deleteDailyPrices(SECRET, FROM, LATEST, null).getResult();

        assertThat(response.getDeletedRows()).isEqualTo(1200);
        assertThat(response.getDeletedSyncLogs()).isEqualTo(5);
        assertThat(response.getStockCode()).isNull();
        assertThat(response.getMessage()).contains("백필");
    }
}
