package com.example.demo.api.stockDailyPrice.service;

import com.example.demo.common.exception.GeneralException;
import com.example.demo.domain.stock.entity.Stock;
import com.example.demo.domain.stock.exception.StockErrorStatus;
import com.example.demo.domain.stock.exception.StockHandler;
import com.example.demo.domain.stock.service.StockQueryService;
import com.example.demo.domain.stockDailyPrice.entity.StockDailyPriceDeleteResult;
import com.example.demo.domain.stockDailyPrice.exception.StockDailyPriceErrorStatus;
import com.example.demo.domain.stockDailyPrice.service.StockDailyPriceCommandService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class StockDailyPriceDeleteUseCaseTest {

    private static final LocalDate FROM = LocalDate.of(2023, 1, 2);
    private static final LocalDate TO = LocalDate.of(2023, 12, 29);

    private final StockQueryService stockQueryService = Mockito.mock(StockQueryService.class);
    private final StockDailyPriceCommandService commandService = Mockito.mock(StockDailyPriceCommandService.class);
    private final DailyPriceJobGuard guard = Mockito.mock(DailyPriceJobGuard.class);
    private final StockDailyPriceDeleteUseCase useCase = new StockDailyPriceDeleteUseCase(stockQueryService, commandService, guard);

    @BeforeEach
    void setUp() {
        Mockito.when(guard.tryAcquire()).thenReturn(Optional.of("token"));
    }

    private static int code(Throwable e) {
        return ((GeneralException) e).getErrorReasonHttpStatus().getCode();
    }

    @Test
    void 전_종목_삭제() {
        Mockito.when(commandService.deleteDailyPrices(FROM, TO, null)).thenReturn(new StockDailyPriceDeleteResult(50_000, 250));

        assertThat(useCase.delete(FROM, TO, null)).isEqualTo(new StockDailyPriceDeleteResult(50_000, 250));
        verify(stockQueryService, never()).getStockByCode(any());
    }

    @Test
    void 한_종목_삭제는_종목_존재를_먼저_확인한다() {
        Mockito.when(stockQueryService.getStockByCode("005930")).thenReturn(Stock.builder().stockCode("005930").build());
        Mockito.when(commandService.deleteDailyPrices(FROM, TO, "005930")).thenReturn(new StockDailyPriceDeleteResult(250, 0));

        assertThat(useCase.delete(FROM, TO, "005930").deletedRows()).isEqualTo(250);
    }

    @Test
    void 없는_종목이면_404_4250이고_지우지_않는다() {
        Mockito.when(stockQueryService.getStockByCode("999999")).thenThrow(StockHandler.notFound());

        assertThatThrownBy(() -> useCase.delete(FROM, TO, "999999"))
                .satisfies(e -> assertThat(code(e)).isEqualTo(StockErrorStatus.STOCK_NOT_FOUND.getCode()));
        verify(commandService, never()).deleteDailyPrices(any(), any(), any());
    }

    @Test
    void from이_to보다_뒤면_4255이고_지우지_않는다() {
        assertThatThrownBy(() -> useCase.delete(TO, FROM, null))
                .satisfies(e -> assertThat(code(e)).isEqualTo(StockDailyPriceErrorStatus.DAILY_PRICE_INVALID_RANGE.getCode()));
        verify(commandService, never()).deleteDailyPrices(any(), any(), any());
    }

    /** 컨트롤러가 필수로 받지만, 유스케이스도 기간 없는 전체 삭제를 막는다. */
    @Test
    void 기간이_비면_4255() {
        assertThatThrownBy(() -> useCase.delete(null, TO, null))
                .satisfies(e -> assertThat(code(e)).isEqualTo(StockDailyPriceErrorStatus.DAILY_PRICE_INVALID_RANGE.getCode()));
    }

    /** 적재 중에 지우면 지운 데이터가 다시 들어가거나 이력과 어긋난다. 적재가 돌고 있으면 거절한다. */
    @Test
    void 적재가_돌고_있으면_409_4258이고_지우지_않는다() {
        Mockito.when(guard.tryAcquire()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.delete(FROM, TO, null))
                .satisfies(e -> assertThat(code(e)).isEqualTo(StockDailyPriceErrorStatus.DAILY_PRICE_JOB_RUNNING.getCode()));
        verify(commandService, never()).deleteDailyPrices(any(), any(), any());
    }

    @Test
    void 지우고_나면_잠금을_푼다() {
        Mockito.when(commandService.deleteDailyPrices(FROM, TO, null)).thenReturn(new StockDailyPriceDeleteResult(1, 1));

        useCase.delete(FROM, TO, null);

        verify(guard).release("token");
    }

    @Test
    void 삭제가_실패해도_잠금을_푼다() {
        Mockito.when(commandService.deleteDailyPrices(FROM, TO, null)).thenThrow(new RuntimeException("db down"));

        assertThatThrownBy(() -> useCase.delete(FROM, TO, null)).isInstanceOf(RuntimeException.class);
        verify(guard).release("token");
    }
}
