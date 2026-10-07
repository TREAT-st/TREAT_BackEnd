package com.example.demo.api.batch.service;

import com.example.demo.domain.batch.entity.BatchExecutionLog;
import com.example.demo.domain.batch.entity.BatchExecutionRef;
import com.example.demo.domain.batch.entity.BatchStatus;
import com.example.demo.domain.batch.entity.BatchStep;
import com.example.demo.domain.batch.repository.BatchExecutionLogRepository;
import com.example.demo.domain.batch.service.BatchExecutionLogService;
import com.example.demo.api.batch.service.ReportUrlRecoverer.RecoveryResult;
import com.example.demo.domain.volatility.service.VolatilityQueryService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * 그레이스를 넘긴 도착 확인의 정리 규칙 검증.
 *
 * 그레이스 경과 판정은 리포지토리 쿼리가 하므로 여기서는 조회 결과를 그대로 주고,
 * 그 뒤의 판단만 본다.
 */
class VerifySweeperTest {

    private static final LocalDate TRADE_DATE = LocalDate.of(2026, 9, 9);

    private final BatchExecutionLogRepository repository = Mockito.mock(BatchExecutionLogRepository.class);
    private final BatchExecutionLogService logService = Mockito.mock(BatchExecutionLogService.class);
    private final VolatilityQueryService volatilityQueryService = Mockito.mock(VolatilityQueryService.class);
    private final ReportUrlRecoverer reportUrlRecoverer = Mockito.mock(ReportUrlRecoverer.class);

    private final VerifySweeper sweeper =
            new VerifySweeper(repository, logService, volatilityQueryService, reportUrlRecoverer);

    @Test
    void 정리할_대상이_없으면_아무것도_하지_않는다() {
        stale(List.of());

        sweeper.sweepStaleVerifications();

        verifyNoInteractions(logService, volatilityQueryService, reportUrlRecoverer);
    }

    /**
     * 리포트는 다 왔는데 RUNNING인 경우가 있다.
     * 마지막 콜백이 도착할 때 앱이 재시작 중이었거나 그 트랜잭션이 실패한 경우다.
     */
    @Test
    void 리포트가_모두_도착해_있으면_성공으로_닫는다() {
        stale(List.of(verification(1L, 1)));
        Mockito.when(volatilityQueryService.countMissingReport(TRADE_DATE)).thenReturn(0L);

        sweeper.sweepStaleVerifications();

        verify(logService).succeed(eq(new BatchExecutionRef(1L, 1)), anyString());
        verify(logService, never()).fail(any(), anyString());
    }

    @Test
    void 미도착이_남아_있으면_실패로_닫는다() {
        stale(List.of(verification(1L, 1)));
        Mockito.when(volatilityQueryService.countMissingReport(TRADE_DATE)).thenReturn(2L);
        Mockito.when(reportUrlRecoverer.recover(TRADE_DATE)).thenReturn(new RecoveryResult(0, 0));

        sweeper.sweepStaleVerifications();

        verify(logService).fail(eq(new BatchExecutionRef(1L, 1)), eq("리포트 2건이 도착하지 않았습니다."));
        verify(logService, never()).succeed(any(), anyString());
    }

    /** 리포트는 만들어졌는데 콜백만 유실된 경우다. 재요청 없이 URL만 채우면 끝난다. */
    @Test
    void S3에서_보정하면_성공으로_닫는다() {
        stale(List.of(verification(1L, 1)));
        Mockito.when(volatilityQueryService.countMissingReport(TRADE_DATE)).thenReturn(2L, 0L);
        Mockito.when(reportUrlRecoverer.recover(TRADE_DATE)).thenReturn(new RecoveryResult(2, 0));

        sweeper.sweepStaleVerifications();

        verify(logService).succeed(eq(new BatchExecutionRef(1L, 1)),
                eq("S3에서 2건을 보정해 리포트가 모두 채워졌습니다."));
        verify(logService, never()).fail(any(), anyString());
    }

    /** 보정 중에 콜백이 나머지를 채울 수 있다. 판정은 반드시 다시 센 값으로 한다. */
    @Test
    void 보정_뒤_다시_센_값으로_판정한다() {
        stale(List.of(verification(1L, 1)));
        Mockito.when(volatilityQueryService.countMissingReport(TRADE_DATE)).thenReturn(3L, 1L);
        Mockito.when(reportUrlRecoverer.recover(TRADE_DATE)).thenReturn(new RecoveryResult(1, 0));

        sweeper.sweepStaleVerifications();

        // 처음 센 3이 아니라 다시 센 1이 메시지에 들어가야 한다.
        verify(logService).fail(eq(new BatchExecutionRef(1L, 1)), eq("리포트 1건이 도착하지 않았습니다."));
    }

    /** 권한 장애를 단순 미도착으로 기록하면 재요청할 일인지 권한을 볼 일인지 갈리지 않는다. */
    @Test
    void S3_조회_실패는_메시지에_따로_남는다() {
        stale(List.of(verification(1L, 1)));
        Mockito.when(volatilityQueryService.countMissingReport(TRADE_DATE)).thenReturn(2L);
        Mockito.when(reportUrlRecoverer.recover(TRADE_DATE)).thenReturn(new RecoveryResult(0, 2));

        sweeper.sweepStaleVerifications();

        verify(logService).fail(eq(new BatchExecutionRef(1L, 1)),
                eq("리포트 2건이 도착하지 않았습니다. S3 조회 실패 2건."));
    }

    /**
     * 조회한 뒤 갱신하기 전에 다른 실행이 재시작할 수 있다.
     * (tradeDate, step)으로 닫으면 새 회차를 잘못 끝내므로 조회한 행의 ref로 닫아야 한다.
     */
    @Test
    void 조회한_회차_그대로_닫는다() {
        stale(List.of(verification(7L, 3)));
        Mockito.when(volatilityQueryService.countMissingReport(TRADE_DATE)).thenReturn(1L);
        Mockito.when(reportUrlRecoverer.recover(TRADE_DATE)).thenReturn(new RecoveryResult(0, 0));

        sweeper.sweepStaleVerifications();

        verify(logService).fail(eq(new BatchExecutionRef(7L, 3)), anyString());
    }

    /** 한 거래일 처리가 터져도 나머지는 계속 봐야 한다. */
    @Test
    void 하나가_실패해도_나머지를_계속_처리한다() {
        BatchExecutionLog first = verification(1L, 1, LocalDate.of(2026, 9, 8));
        BatchExecutionLog second = verification(2L, 1, TRADE_DATE);
        stale(List.of(first, second));

        Mockito.when(volatilityQueryService.countMissingReport(LocalDate.of(2026, 9, 8)))
                .thenThrow(new IllegalStateException("조회 실패"));
        Mockito.when(volatilityQueryService.countMissingReport(TRADE_DATE)).thenReturn(0L);

        sweeper.sweepStaleVerifications();

        verify(logService).succeed(eq(new BatchExecutionRef(2L, 1)), anyString());
    }

    private void stale(List<BatchExecutionLog> verifications) {
        Mockito.when(repository.findAllByStepAndStatusAndStartedAtBefore(
                        eq(BatchStep.VERIFY), eq(BatchStatus.RUNNING), any(LocalDateTime.class)))
                .thenReturn(verifications);
    }

    private BatchExecutionLog verification(Long id, int attempt) {
        return verification(id, attempt, TRADE_DATE);
    }

    private BatchExecutionLog verification(Long id, int attempt, LocalDate tradeDate) {
        return BatchExecutionLog.builder()
                .id(id)
                .tradeDate(tradeDate)
                .step(BatchStep.VERIFY)
                .status(BatchStatus.RUNNING)
                .attempt(attempt)
                .startedAt(LocalDateTime.of(2026, 9, 9, 16, 8))
                .build();
    }
}
