package com.example.demo.api.batch.service;

import com.example.demo.api.batch.dto.BatchResponseDto.DailyBatchResult;
import com.example.demo.api.stock.dto.StockResponseDto.SyncStocksResponse;
import com.example.demo.api.stock.service.StockUseCase;
import com.example.demo.api.volatility.dto.VolatilityResponseDto.DetectionResult;
import com.example.demo.api.volatility.dto.VolatilityResponseDto.ReportGenerationResult;
import com.example.demo.api.volatility.service.VolatilityUseCase;
import com.example.demo.domain.batch.entity.BatchExecutionRef;
import com.example.demo.domain.batch.entity.BatchStartResult;
import com.example.demo.domain.batch.entity.BatchStep;
import com.example.demo.domain.batch.entity.DailyBatchOutcome;
import com.example.demo.domain.batch.service.BatchExecutionLogService;
import com.example.demo.domain.stock.entity.Kospi200SyncCommand;
import com.example.demo.domain.stock.exception.StockHandler;
import com.example.demo.domain.volatility.entity.Volatility;
import com.example.demo.domain.volatility.exception.VolatilityHandler;
import com.example.demo.domain.volatility.service.VolatilityQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static com.example.demo.common.consts.StaticVariable.SEOUL_ZONE;
import static com.example.demo.common.consts.StaticVariable.SYNC_ALREADY_APPLIED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * 체인의 순서와 중단 규칙 검증.
 *
 * 배치는 실행일이 아니라 KRX가 준 거래일을 단계 키로 쓴다. Lambda가 오늘을 제외하므로
 * 테스트의 거래일도 실행일 전날로 둔다. 실행일 키는 거래일을 확보하지 못한 시작 실패에만 쓰인다.
 */
class BatchUseCaseTest {

    /** 실행일. 거래일을 확보하지 못한 시작 실패만 이 키로 남는다. */
    private static final LocalDate TODAY = LocalDate.now(SEOUL_ZONE);
    /** KRX가 준 거래일. Lambda가 오늘을 제외하므로 실행일보다 앞선다. */
    private static final LocalDate TRADE_DATE = TODAY.minusDays(1);

    private final StockUseCase stockUseCase = Mockito.mock(StockUseCase.class);
    private final VolatilityUseCase volatilityUseCase = Mockito.mock(VolatilityUseCase.class);
    private final VolatilityQueryService volatilityQueryService = Mockito.mock(VolatilityQueryService.class);
    private final BatchExecutionLogService batchExecutionLogService = Mockito.mock(BatchExecutionLogService.class);

    private final BatchUseCase useCase = new BatchUseCase(
            stockUseCase, volatilityUseCase, volatilityQueryService, batchExecutionLogService);

    /** 실행 이력 id는 단계마다 달라야 성공/실패가 엉뚱한 행에 붙지 않는다. */
    private static final long VERIFY_ID = 400L;

    private final AtomicInteger executionId = new AtomicInteger();

    @BeforeEach
    void setUp() {
        Mockito.when(stockUseCase.fetchKospi200()).thenReturn(command(TRADE_DATE));
        Mockito.when(stockUseCase.syncKospi200(any(), anyBoolean())).thenReturn(syncResponse());
        Mockito.when(volatilityUseCase.runDetection(any(LocalDate.class))).thenReturn(detectionResult(10));
        Mockito.when(volatilityUseCase.runReportGeneration(any(), any(LocalDate.class)))
                .thenReturn(reportResult(10, 0));
        Mockito.when(volatilityQueryService.getByTradeDate(TRADE_DATE)).thenReturn(List.of(volatility()));
        allStepsStart();
    }

    @Test
    void 정상_실행이면_세_단계가_순서대로_기록된다() {
        DailyBatchResult result = useCase.runDailyBatch(false);

        assertThat(result.getOutcome()).isEqualTo(DailyBatchOutcome.COMPLETED);
        assertThat(result.getTradeDate()).isEqualTo(TRADE_DATE);

        InOrder inOrder = Mockito.inOrder(stockUseCase, volatilityUseCase, batchExecutionLogService);
        inOrder.verify(batchExecutionLogService).tryStart(TRADE_DATE, BatchStep.SYNC, false);
        inOrder.verify(stockUseCase).syncKospi200(any(), anyBoolean());
        inOrder.verify(batchExecutionLogService).succeed(any(), anyString());
        inOrder.verify(batchExecutionLogService).tryStart(TRADE_DATE, BatchStep.DETECT, false);
        inOrder.verify(volatilityUseCase).runDetection(TRADE_DATE);
        inOrder.verify(batchExecutionLogService).succeed(any(), anyString());
        inOrder.verify(batchExecutionLogService).tryStart(TRADE_DATE, BatchStep.REPORT, false);
        inOrder.verify(volatilityUseCase).runReportGeneration(any(), eq(TRADE_DATE));
        inOrder.verify(batchExecutionLogService).succeed(any(), anyString());
        // 도착 확인은 체인이 끝난 뒤에 일어난다. 열어두기만 하고 닫지 않는다.
        inOrder.verify(batchExecutionLogService).tryStart(TRADE_DATE, BatchStep.VERIFY, false);
    }

    /**
     * VERIFY는 체인이 실행하는 단계가 아니라 넘겨주는 단계다.
     * 리포트는 ECS가 만들어 콜백으로 알려주므로 체인이 끝난 뒤에야 도착한다.
     */
    @Test
    void REPORT_성공_후_VERIFY를_열어두고_끝낸다() {
        DailyBatchResult result = useCase.runDailyBatch(false);

        assertThat(result.getOutcome()).isEqualTo(DailyBatchOutcome.COMPLETED);
        verify(batchExecutionLogService).tryStart(TRADE_DATE, BatchStep.VERIFY, false);
        // 닫는 건 Phase 4의 콜백과 스위퍼가 한다. 체인은 손대지 않는다.
        verify(batchExecutionLogService, never()).succeed(argThat(ref -> ref.executionId() == VERIFY_ID), anyString());
    }

    /**
     * VERIFY가 아직 기다리는 중이어도 체인은 정상 종료다.
     * 다른 단계처럼 IN_PROGRESS에서 중단하면, 콜백을 기다리는 동안 재실행이 전부 ABORTED가 된다.
     */
    @Test
    void VERIFY가_이미_열려_있어도_체인은_정상_종료된다() {
        Mockito.when(batchExecutionLogService.tryStart(TRADE_DATE, BatchStep.VERIFY, false))
                .thenReturn(BatchStartResult.inProgress());

        DailyBatchResult result = useCase.runDailyBatch(false);

        assertThat(result.getOutcome()).isEqualTo(DailyBatchOutcome.COMPLETED);
        assertThat(result.getFailedStep()).isNull();
    }

    /**
     * Lambda가 오늘을 제외하므로 거래일이 실행일보다 앞서는 게 정상이다.
     * 예전처럼 "거래일 != 실행일"을 휴장일로 보면 매일 아무것도 하지 않는다.
     */
    @Test
    void 거래일이_실행일과_달라도_그_거래일로_실행한다() {
        LocalDate friday = TODAY.minusDays(3);
        Mockito.when(stockUseCase.fetchKospi200()).thenReturn(command(friday));
        for (BatchStep step : BatchStep.values()) {
            Mockito.when(batchExecutionLogService.tryStart(friday, step, false))
                    .thenReturn(BatchStartResult.started(new BatchExecutionRef((long) executionId.incrementAndGet(), 1)));
        }
        Mockito.when(volatilityQueryService.getByTradeDate(friday)).thenReturn(List.of(volatility()));

        DailyBatchResult result = useCase.runDailyBatch(false);

        assertThat(result.getOutcome()).isEqualTo(DailyBatchOutcome.COMPLETED);
        assertThat(result.getTradeDate()).isEqualTo(friday);
        verify(stockUseCase).syncKospi200(any(), eq(false));
        verify(volatilityUseCase).runDetection(friday);
        verify(batchExecutionLogService, never()).skip(any(), eq(BatchStep.SYNC), anyString());
    }

    /**
     * 휴장일에는 직전 거래일이 다시 내려온다. 그 거래일의 단계는 이미 성공했으므로
     * 전부 ALREADY_DONE으로 지나가고, 실제 동기화·탐지·리포트는 하나도 일어나지 않는다.
     */
    @Test
    void 이미_처리한_거래일이_다시_오면_아무것도_실행하지_않는다() {
        for (BatchStep step : BatchStep.values()) {
            Mockito.when(batchExecutionLogService.tryStart(TRADE_DATE, step, false))
                    .thenReturn(BatchStartResult.alreadyDone());
        }

        DailyBatchResult result = useCase.runDailyBatch(false);

        assertThat(result.getOutcome()).isEqualTo(DailyBatchOutcome.COMPLETED);
        verify(stockUseCase, never()).syncKospi200(any(), anyBoolean());
        verify(volatilityUseCase, never()).runDetection(any(LocalDate.class));
        verify(volatilityUseCase, never()).runReportGeneration(any(), any(LocalDate.class));
    }

    /**
     * 수동 /sync가 먼저 돌았으면 stock은 이미 그 거래일 상태다(4252).
     * 실패로 남기면 체인이 멈춰 DETECT·REPORT가 영영 돌지 않는다.
     */
    @Test
    void 이미_반영된_거래일_4252는_SYNC_성공으로_보고_DETECT로_진행한다() {
        Mockito.when(stockUseCase.syncKospi200(any(), anyBoolean()))
                .thenThrow(StockHandler.tradeDateAlreadySynced());

        DailyBatchResult result = useCase.runDailyBatch(false);

        assertThat(result.getOutcome()).isEqualTo(DailyBatchOutcome.COMPLETED);
        verify(batchExecutionLogService).succeed(any(), eq(SYNC_ALREADY_APPLIED));
        verify(batchExecutionLogService, never()).fail(any(), anyString());
        verify(volatilityUseCase).runDetection(TRADE_DATE);
    }

    /** 4253도 409지만 Lambda가 오래된 응답을 줬다는 신호다. 성공으로 삼키면 안 된다. */
    @Test
    void 과거_거래일_4253은_SYNC_실패로_남는다() {
        Mockito.when(stockUseCase.syncKospi200(any(), anyBoolean()))
                .thenThrow(StockHandler.staleTradeDate());

        DailyBatchResult result = useCase.runDailyBatch(false);

        assertThat(result.getOutcome()).isEqualTo(DailyBatchOutcome.ABORTED);
        assertThat(result.getFailedStep()).isEqualTo(BatchStep.SYNC);
        verify(batchExecutionLogService).fail(any(), anyString());
        verifyNoInteractions(volatilityUseCase);
    }

    @Test
    void 잘못된_거래일_4254는_SYNC_실패로_남는다() {
        Mockito.when(stockUseCase.syncKospi200(any(), anyBoolean()))
                .thenThrow(StockHandler.invalidTradeDate());

        DailyBatchResult result = useCase.runDailyBatch(false);

        assertThat(result.getOutcome()).isEqualTo(DailyBatchOutcome.ABORTED);
        assertThat(result.getFailedStep()).isEqualTo(BatchStep.SYNC);
        verifyNoInteractions(volatilityUseCase);
    }

    /** 탐지 거래일이 어긋나면 저장 전에 멈춘다. REPORT로 넘어가면 대상 0건으로 조용히 끝난다. */
    @Test
    void DETECT_거래일이_어긋나면_DETECT_실패로_중단된다() {
        Mockito.when(volatilityUseCase.runDetection(any(LocalDate.class)))
                .thenThrow(VolatilityHandler.tradeDateMismatch());

        DailyBatchResult result = useCase.runDailyBatch(false);

        assertThat(result.getOutcome()).isEqualTo(DailyBatchOutcome.ABORTED);
        assertThat(result.getFailedStep()).isEqualTo(BatchStep.DETECT);
        verify(volatilityUseCase, never()).runReportGeneration(any(), any(LocalDate.class));
    }

    /**
     * KRX 조회가 실패하면 거래일을 모른다. 그래도 실행일 키로 남겨야
     * 조회했을 때 실행 자체가 없었던 것과 구분된다.
     */
    @Test
    void 거래일을_확보하지_못하면_실행일에_실패로_남긴다() {
        Mockito.when(batchExecutionLogService.tryStart(TODAY, BatchStep.SYNC, false))
                .thenReturn(BatchStartResult.started(new BatchExecutionRef(999L, 1)));
        Mockito.when(stockUseCase.fetchKospi200())
                .thenThrow(new IllegalStateException("KRX Lambda 호출 실패"));

        DailyBatchResult result = useCase.runDailyBatch(false);

        assertThat(result.getOutcome()).isEqualTo(DailyBatchOutcome.ABORTED);
        assertThat(result.getTradeDate()).isNull();
        assertThat(result.getFailedStep()).isNull();
        assertThat(result.getMessage()).isEqualTo("KRX Lambda 호출 실패");

        verify(batchExecutionLogService).tryStart(TODAY, BatchStep.SYNC, false);
        verify(batchExecutionLogService).fail(any(), eq("KRX Lambda 호출 실패"));
        verify(stockUseCase, never()).syncKospi200(any(), anyBoolean());
    }

    /**
     * 강제 재실행 중 KRX가 죽었다고 이미 성공한 SYNC를 되돌리면 안 된다.
     * 시작 실패 기록은 force를 넘기지 않고, STARTED가 아니면 손대지 않는다.
     */
    @Test
    void 이미_끝난_SYNC는_시작_실패로_덮이지_않는다() {
        Mockito.when(batchExecutionLogService.tryStart(TODAY, BatchStep.SYNC, false))
                .thenReturn(BatchStartResult.alreadyDone());
        Mockito.when(stockUseCase.fetchKospi200())
                .thenThrow(new IllegalStateException("KRX Lambda 호출 실패"));

        DailyBatchResult result = useCase.runDailyBatch(true);

        assertThat(result.getOutcome()).isEqualTo(DailyBatchOutcome.ABORTED);
        // execution()이 null이라 그대로 넘기면 터진다. 애초에 호출하지 않아야 한다.
        verify(batchExecutionLogService, never()).fail(any(), anyString());
    }

    /** 메시지 없는 예외가 그대로 넘어가면 실행 이력에 FAILED만 남고 사유가 빈다. */
    @Test
    void 예외_메시지가_없어도_사유가_기록된다() {
        Mockito.when(stockUseCase.syncKospi200(any(), anyBoolean())).thenThrow(new IllegalStateException());

        DailyBatchResult result = useCase.runDailyBatch(false);

        assertThat(result.getMessage()).isEqualTo("IllegalStateException");
        verify(batchExecutionLogService).fail(any(), eq("IllegalStateException"));
    }

    /**
     * 일부 종목의 요청이 실패해도 단계는 성공이다. FAILED로 남기면 재실행이 이미 요청한 종목까지
     * 다시 요청해 GPT 비용이 두 배가 된다. 대신 어느 종목이 못 나갔는지는 이력에 남아야 한다.
     */
    @Test
    void 리포트_요청이_일부_실패해도_단계는_성공이고_실패_종목이_남는다() {
        Mockito.when(volatilityUseCase.runReportGeneration(any(), any(LocalDate.class)))
                .thenReturn(reportResult(10, 2));

        DailyBatchResult result = useCase.runDailyBatch(false);

        assertThat(result.getOutcome()).isEqualTo(DailyBatchOutcome.COMPLETED);
        verify(batchExecutionLogService, never()).fail(any(), anyString());

        ArgumentCaptor<String> summary = ArgumentCaptor.forClass(String.class);
        verify(batchExecutionLogService, atLeastOnce()).succeed(any(), summary.capture());
        assertThat(summary.getAllValues())
                .anySatisfy(s -> assertThat(s).contains("요청 실패 2건").contains("005930"));
        // 못 나간 요청은 도착 확인 단계가 잡아야 하므로 VERIFY는 반드시 열려야 한다.
        verify(batchExecutionLogService).tryStart(TRADE_DATE, BatchStep.VERIFY, false);
    }

    @Test
    void SYNC가_실패하면_DETECT는_시작되지_않는다() {
        Mockito.when(stockUseCase.syncKospi200(any(), anyBoolean())).thenThrow(new IllegalStateException("KRX 반영 실패"));

        DailyBatchResult result = useCase.runDailyBatch(false);

        assertThat(result.getOutcome()).isEqualTo(DailyBatchOutcome.ABORTED);
        assertThat(result.getFailedStep()).isEqualTo(BatchStep.SYNC);
        assertThat(result.getMessage()).isEqualTo("KRX 반영 실패");

        verify(batchExecutionLogService).fail(any(), eq("KRX 반영 실패"));
        verify(batchExecutionLogService, never()).tryStart(any(), eq(BatchStep.DETECT), anyBoolean());
        verifyNoInteractions(volatilityUseCase);
    }

    /** 재실행에서 앞 단계를 다시 돌리지 않는 것이 이 체인의 목적이다. */
    @Test
    void 이미_성공한_단계는_실행하지_않고_다음으로_진행한다() {
        Mockito.when(batchExecutionLogService.tryStart(TRADE_DATE, BatchStep.SYNC, false))
                .thenReturn(BatchStartResult.alreadyDone());

        DailyBatchResult result = useCase.runDailyBatch(false);

        assertThat(result.getOutcome()).isEqualTo(DailyBatchOutcome.COMPLETED);
        verify(stockUseCase, never()).syncKospi200(any(), anyBoolean());
        verify(volatilityUseCase).runDetection(TRADE_DATE);
    }

    @Test
    void 진행_중인_실행이_있으면_체인이_중단된다() {
        Mockito.when(batchExecutionLogService.tryStart(TRADE_DATE, BatchStep.DETECT, false))
                .thenReturn(BatchStartResult.inProgress());

        DailyBatchResult result = useCase.runDailyBatch(false);

        assertThat(result.getOutcome()).isEqualTo(DailyBatchOutcome.ABORTED);
        assertThat(result.getFailedStep()).isEqualTo(BatchStep.DETECT);
        verify(volatilityUseCase, never()).runDetection(any(LocalDate.class));
        verify(batchExecutionLogService, never()).tryStart(any(), eq(BatchStep.REPORT), anyBoolean());
    }

    /** 조용한 장세에는 실제로 0건이 나온다. 실패로 기록하면 정상 상황에 알람이 울린다. */
    @Test
    void 탐지된_종목이_없으면_REPORT는_스킵된다() {
        Mockito.when(volatilityUseCase.runDetection(any(LocalDate.class))).thenReturn(detectionResult(0));
        Mockito.when(volatilityQueryService.getByTradeDate(TRADE_DATE)).thenReturn(List.of());

        DailyBatchResult result = useCase.runDailyBatch(false);

        assertThat(result.getOutcome()).isEqualTo(DailyBatchOutcome.COMPLETED);
        verify(batchExecutionLogService).skip(TRADE_DATE, BatchStep.REPORT, "탐지된 종목 없음");
        verify(batchExecutionLogService, never()).tryStart(any(), eq(BatchStep.REPORT), anyBoolean());
        verify(volatilityUseCase, never()).runReportGeneration(any(), any(LocalDate.class));
        // 요청한 리포트가 없으니 도착을 확인할 것도 없다.
        verify(batchExecutionLogService, never()).tryStart(any(), eq(BatchStep.VERIFY), anyBoolean());
    }

    /**
     * 탐지가 0건이면 저장 단계가 기존 기록을 지우지 않고 유지한다(reportUrl 보호).
     * 그 상태에서 DB만 보고 판정하면 최신 탐지가 제외한 종목으로 리포트를 요청하게 된다.
     */
    @Test
    void 탐지가_0건이면_기존_기록이_남아_있어도_REPORT를_건너뛴다() {
        Mockito.when(volatilityUseCase.runDetection(any(LocalDate.class))).thenReturn(detectionResult(0));
        Mockito.when(volatilityQueryService.getByTradeDate(TRADE_DATE)).thenReturn(List.of(volatility()));

        DailyBatchResult result = useCase.runDailyBatch(false);

        assertThat(result.getOutcome()).isEqualTo(DailyBatchOutcome.COMPLETED);
        verify(batchExecutionLogService).skip(TRADE_DATE, BatchStep.REPORT, "탐지된 종목 없음");
        verify(volatilityUseCase, never()).runReportGeneration(any(), any(LocalDate.class));
    }

    /**
     * DETECT를 건너뛴 재실행에는 이번 탐지 결과가 없다.
     * 그때는 지난 실행이 남긴 DB가 가장 최근 판단이다.
     */
    @Test
    void DETECT를_건너뛴_재실행은_DB를_근거로_REPORT_대상을_판정한다() {
        Mockito.when(batchExecutionLogService.tryStart(TRADE_DATE, BatchStep.DETECT, false))
                .thenReturn(BatchStartResult.alreadyDone());
        Mockito.when(volatilityQueryService.getByTradeDate(TRADE_DATE)).thenReturn(List.of(volatility()));

        DailyBatchResult result = useCase.runDailyBatch(false);

        assertThat(result.getOutcome()).isEqualTo(DailyBatchOutcome.COMPLETED);
        verify(volatilityUseCase, never()).runDetection(any(LocalDate.class));
        verify(volatilityUseCase).runReportGeneration(any(), eq(TRADE_DATE));
    }

    /** REPORT 중복 실행은 GPT 비용이 그대로 두 배가 되는 지점이라 직접 고정한다. */
    @Test
    void REPORT가_이미_성공했으면_요청_없이_completed로_끝난다() {
        Mockito.when(batchExecutionLogService.tryStart(TRADE_DATE, BatchStep.REPORT, false))
                .thenReturn(BatchStartResult.alreadyDone());

        DailyBatchResult result = useCase.runDailyBatch(false);

        assertThat(result.getOutcome()).isEqualTo(DailyBatchOutcome.COMPLETED);
        verify(volatilityUseCase, never()).runReportGeneration(any(), any(LocalDate.class));
    }

    @Test
    void REPORT가_진행_중이면_요청_없이_중단된다() {
        Mockito.when(batchExecutionLogService.tryStart(TRADE_DATE, BatchStep.REPORT, false))
                .thenReturn(BatchStartResult.inProgress());

        DailyBatchResult result = useCase.runDailyBatch(false);

        assertThat(result.getOutcome()).isEqualTo(DailyBatchOutcome.ABORTED);
        assertThat(result.getFailedStep()).isEqualTo(BatchStep.REPORT);
        verify(volatilityUseCase, never()).runReportGeneration(any(), any(LocalDate.class));
    }

    /** force는 실행 이력 서비스로 그대로 내려가야 한다. 여기서 삼키면 강제 재실행이 동작하지 않는다. */
    @Test
    void force는_모든_단계에_전달된다() {
        allStepsStart(true);

        useCase.runDailyBatch(true);

        verify(batchExecutionLogService).tryStart(TRADE_DATE, BatchStep.SYNC, true);
        verify(batchExecutionLogService).tryStart(TRADE_DATE, BatchStep.DETECT, true);
        verify(batchExecutionLogService).tryStart(TRADE_DATE, BatchStep.REPORT, true);
        // 실행 이력만 재시작하고 stock 커맨드에 force가 빠지면 4252에 막혀 실제 재반영이 안 된다.
        verify(stockUseCase).syncKospi200(any(), eq(true));
    }

    @Test
    void force가_아니면_stock_커맨드에도_force가_넘어가지_않는다() {
        useCase.runDailyBatch(false);

        verify(stockUseCase).syncKospi200(any(), eq(false));
    }

    private void allStepsStart() {
        allStepsStart(false);
    }

    private void allStepsStart(boolean force) {
        for (BatchStep step : BatchStep.values()) {
            long id = step == BatchStep.VERIFY ? VERIFY_ID : executionId.incrementAndGet();
            Mockito.when(batchExecutionLogService.tryStart(TRADE_DATE, step, force))
                    .thenReturn(BatchStartResult.started(new BatchExecutionRef(id, 1)));
        }
    }

    private Kospi200SyncCommand command(LocalDate tradeDate) {
        return new Kospi200SyncCommand(tradeDate, Map.of("005930", "삼성전자"), Set.of(), List.of(), List.of());
    }

    private SyncStocksResponse syncResponse() {
        return SyncStocksResponse.builder()
                .tradeDate(TRADE_DATE)
                .addedCount(1)
                .updatedCount(2)
                .priceUpdatedCount(198)
                .build();
    }

    private DetectionResult detectionResult(int detectedCount) {
        return DetectionResult.builder()
                .tradeDate(TRADE_DATE)
                .stocks(List.of())
                .detectedCount(detectedCount)
                .build();
    }

    private ReportGenerationResult reportResult(int requested, int failed) {
        return ReportGenerationResult.builder()
                .tradeDate(TRADE_DATE)
                .requestedCount(requested)
                .failedCount(failed)
                .failedStockCodes(failed == 0 ? List.of() : List.of("005930", "000660"))
                .build();
    }

    private Volatility volatility() {
        return Volatility.builder()
                .stockCode("005930")
                .stockName("삼성전자")
                .tradeDate(TRADE_DATE)
                .build();
    }
}
