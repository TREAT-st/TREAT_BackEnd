package com.example.demo.api.batch.service;

import com.example.demo.domain.batch.entity.BatchExecutionLog;
import com.example.demo.domain.batch.entity.BatchExecutionRef;
import com.example.demo.domain.batch.entity.BatchStatus;
import com.example.demo.domain.batch.entity.BatchStep;
import com.example.demo.domain.batch.repository.BatchExecutionLogRepository;
import com.example.demo.domain.batch.service.BatchExecutionLogService;
import com.example.demo.api.batch.service.ReportUrlRecoverer.RecoveryResult;
import com.example.demo.domain.volatility.service.VolatilityQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static com.example.demo.common.consts.StaticVariable.SEOUL_ZONE;
import static com.example.demo.common.consts.StaticVariable.VERIFY_ALL_REPORTS_ARRIVED;
import static com.example.demo.common.consts.StaticVariable.VERIFY_MISSING_REPORT;
import static com.example.demo.common.consts.StaticVariable.VERIFY_RECOVERED_FROM_S3;
import static com.example.demo.common.consts.StaticVariable.VERIFY_S3_LOOKUP_FAILED;

/**
 * 도착 확인이 끝나지 않은 VERIFY를 정리한다.
 *
 * 정상 경로는 콜백이 마지막 리포트를 채우면서 닫는 것이다. 하지만 콜백이 하나라도 유실되면
 * 아무도 닫지 않아 VERIFY가 영원히 RUNNING으로 남는다. 그걸 여기서 끊는다.
 *
 * 리포트가 다 와 있는데 RUNNING인 경우도 있다. 마지막 콜백이 도착할 때 앱이 재시작 중이었거나
 * 그 트랜잭션이 실패한 경우다. 그때는 실패가 아니라 성공으로 닫는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VerifySweeper {

    /**
     * 이 시간이 지나도 리포트가 안 왔으면 미도착으로 판정한다.
     * ECS 태스크가 50분쯤 걸리므로 여유를 더한 값이다.
     *
     * {@code BatchStep.VERIFY.runningTimeout}(180분)과 다른 값이고 뜻도 다르다.
     * 그쪽은 "재실행이 이 VERIFY를 인계해도 되나"이고 이쪽은 "이제 미도착으로 봐도 되나"다.
     * 판정이 먼저 나야 인계 대상이 될 이유가 없어지므로 이 값이 더 짧아야 한다.
     */
    private static final Duration VERIFY_GRACE = Duration.ofMinutes(80);

    /**
     * 10분마다 돈다. 그레이스가 80분이라 이 해상도면 충분하다.
     * 기동 직후 5분은 건너뛴다. 그때는 정리할 것도 없고 부팅만 무거워진다.
     *
     * @Scheduled의 값은 컴파일 상수여야 해서 Duration을 쓸 수 없다. ISO-8601 문자열로 적는다.
     */
    private static final String SWEEP_INTERVAL = "PT10M";
    private static final String SWEEP_INITIAL_DELAY = "PT5M";

    private final BatchExecutionLogRepository batchExecutionLogRepository;
    private final BatchExecutionLogService batchExecutionLogService;
    private final VolatilityQueryService volatilityQueryService;
    private final ReportUrlRecoverer reportUrlRecoverer;

    @Scheduled(fixedDelayString = SWEEP_INTERVAL, initialDelayString = SWEEP_INITIAL_DELAY)
    public void sweepStaleVerifications() {
        LocalDateTime deadline = LocalDateTime.now(SEOUL_ZONE).minus(VERIFY_GRACE);

        List<BatchExecutionLog> stale = batchExecutionLogRepository
                .findAllByStepAndStatusAndStartedAtBefore(BatchStep.VERIFY, BatchStatus.RUNNING, deadline);
        if (stale.isEmpty()) {
            return;
        }

        log.info("도착 확인이 끝나지 않은 거래일 {}건을 정리합니다.", stale.size());
        for (BatchExecutionLog verify : stale) {
            // 한 거래일 처리가 실패해도 나머지는 계속 본다.
            try {
                sweep(verify);
            } catch (Exception e) {
                log.error("도착 확인 정리에 실패했습니다. tradeDate={}", verify.getTradeDate(), e);
            }
        }
    }

    /**
     * 조회한 시점과 갱신하는 시점 사이에 다른 실행이 VERIFY를 재시작할 수 있다.
     * 그래서 (tradeDate, step)이 아니라 조회한 행의 ref로 닫는다. 회차가 어긋나면 반영되지 않는다.
     */
    private void sweep(BatchExecutionLog verify) {
        LocalDate tradeDate = verify.getTradeDate();
        BatchExecutionRef ref = verify.ref();

        if (volatilityQueryService.countMissingReport(tradeDate) == 0) {
            log.info("리포트가 모두 도착해 있어 도착 확인을 닫습니다. tradeDate={}", tradeDate);
            batchExecutionLogService.succeed(ref, VERIFY_ALL_REPORTS_ARRIVED);
            return;
        }

        // 리포트는 만들어졌는데 콜백만 유실된 경우가 있다. 재요청보다 먼저 S3를 본다.
        RecoveryResult recovery = reportUrlRecoverer.recover(tradeDate);

        // 보정 뒤에는 반드시 다시 센다. 보정 중에 콜백이 더 채웠을 수도 있다.
        long remaining = volatilityQueryService.countMissingReport(tradeDate);
        if (remaining == 0) {
            log.info("S3 보정으로 리포트가 모두 채워졌습니다. tradeDate={} 보정={}건",
                    tradeDate, recovery.recovered());
            batchExecutionLogService.succeed(ref, recovery.recovered() > 0
                    ? VERIFY_RECOVERED_FROM_S3.formatted(recovery.recovered())
                    : VERIFY_ALL_REPORTS_ARRIVED);
            return;
        }

        log.warn("그레이스가 지나도 리포트가 도착하지 않았습니다. tradeDate={} 미도착={}건 조회실패={}건",
                tradeDate, remaining, recovery.lookupFailures());
        batchExecutionLogService.fail(ref, describeMissing(remaining, recovery));
    }

    /**
     * 조회 실패를 묻어두면 권한 장애가 단순 미도착으로 기록된다.
     * 운영자가 재요청할 일인지 권한을 볼 일인지 메시지만 보고 갈라야 한다.
     */
    private String describeMissing(long remaining, RecoveryResult recovery) {
        String message = VERIFY_MISSING_REPORT.formatted(remaining);
        if (recovery.lookupFailures() == 0) {
            return message;
        }
        return message + VERIFY_S3_LOOKUP_FAILED.formatted(recovery.lookupFailures());
    }
}
