package com.example.demo.api.batch.controller;

import com.example.demo.api.batch.dto.BatchResponseDto.BatchExecutionListResponse;
import com.example.demo.api.batch.dto.BatchResponseDto.DailyBatchTriggerResponse;
import com.example.demo.api.common.dto.ApiResponseDto;
import com.example.demo.api.batch.service.BatchUseCase;
import com.example.demo.common.exception.GeneralException;
import com.example.demo.domain.batch.entity.BatchExecutionLog;
import com.example.demo.domain.batch.entity.BatchStatus;
import com.example.demo.domain.batch.entity.BatchStep;
import com.example.demo.domain.batch.exception.BatchErrorStatus;
import com.example.demo.domain.batch.service.BatchExecutionLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 스프링 컨텍스트 없이 컨트롤러만 검증한다.
 * ResponseEntity를 반환하므로 상태 코드도 직접 확인할 수 있어 MockMvc가 필요 없다.
 */
class BatchControllerTest {

    private static final String SECRET = "test-batch-secret";
    private static final LocalDate TRADE_DATE = LocalDate.of(2026, 9, 9);

    private final BatchUseCase useCase = Mockito.mock(BatchUseCase.class);
    private final BatchExecutionLogService logService = Mockito.mock(BatchExecutionLogService.class);

    private final BatchController controller = new BatchController(useCase, logService);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(controller, "triggerSecret", SECRET);
    }

    @Test
    void 시크릿이_없으면_거부한다() {
        assertThatThrownBy(() -> controller.triggerDailyExecution(null, false))
                .isInstanceOf(GeneralException.class)
                .extracting(e -> ((GeneralException) e).getErrorReasonHttpStatus().getCode())
                .isEqualTo(BatchErrorStatus.BATCH_TRIGGER_UNAUTHORIZED.getCode());

        verify(useCase, never()).runDailyBatchAsync(anyBoolean());
    }

    @Test
    void 시크릿이_틀리면_거부한다() {
        assertThatThrownBy(() -> controller.triggerDailyExecution("wrong", false))
                .isInstanceOf(GeneralException.class);

        verify(useCase, never()).runDailyBatchAsync(anyBoolean());
    }

    /** 체인이 수 분 걸리므로 접수만 하고 돌려보낸다. 결과는 실행 이력으로 확인한다. */
    @Test
    void 정상_요청은_202로_접수하고_비동기로_넘긴다() {
        ResponseEntity<ApiResponseDto<DailyBatchTriggerResponse>> response =
                controller.triggerDailyExecution(SECRET, false);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getResult().isAccepted()).isTrue();
        verify(useCase).runDailyBatchAsync(false);
    }

    @Test
    void force는_그대로_전달된다() {
        controller.triggerDailyExecution(SECRET, true);

        verify(useCase).runDailyBatchAsync(true);
    }

    /**
     * 중복 트리거는 실패가 아니다. 4xx로 돌려주면 자동화 계층이 재시도하고,
     * 인스턴스가 둘 이상이면 두 번째 서버는 202가 나가 같은 상황에 응답이 갈린다.
     */
    @Test
    void 이미_실행_중이어도_202이고_접수되지_않았음을_본문에_담는다() {
        doThrow(new TaskRejectedException("busy")).when(useCase).runDailyBatchAsync(anyBoolean());

        ResponseEntity<ApiResponseDto<DailyBatchTriggerResponse>> response =
                controller.triggerDailyExecution(SECRET, false);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody().getResult().isAccepted()).isFalse();
        assertThat(response.getBody().getResult().getMessage()).contains("이미 실행 중");
    }

    @Test
    void 실행_이력을_거래일로_조회한다() {
        Mockito.when(logService.getByTradeDate(TRADE_DATE)).thenReturn(List.of(
                execution(BatchStep.SYNC, BatchStatus.SUCCESS, 1),
                execution(BatchStep.DETECT, BatchStatus.FAILED, 2)));

        BatchExecutionListResponse response =
                controller.getDailyExecutions(SECRET, TRADE_DATE).getResult();

        assertThat(response.getTradeDate()).isEqualTo(TRADE_DATE);
        assertThat(response.getSteps())
                .extracting(s -> s.getStep() + ":" + s.getStatus() + ":" + s.getAttempt())
                .containsExactly("SYNC:SUCCESS:1", "DETECT:FAILED:2");
    }

    @Test
    void 조회도_시크릿을_요구한다() {
        assertThatThrownBy(() -> controller.getDailyExecutions(null, TRADE_DATE))
                .isInstanceOf(GeneralException.class);

        verify(logService, never()).getByTradeDate(TRADE_DATE);
    }

    private BatchExecutionLog execution(BatchStep step, BatchStatus status, int attempt) {
        return BatchExecutionLog.builder()
                .tradeDate(TRADE_DATE)
                .step(step)
                .status(status)
                .attempt(attempt)
                .startedAt(LocalDateTime.of(2026, 9, 9, 16, 0))
                .build();
    }
}
