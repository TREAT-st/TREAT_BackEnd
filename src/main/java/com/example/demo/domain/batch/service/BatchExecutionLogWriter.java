package com.example.demo.domain.batch.service;

import com.example.demo.domain.batch.entity.BatchExecutionLog;
import com.example.demo.domain.batch.entity.BatchExecutionRef;
import com.example.demo.domain.batch.entity.BatchStartResult;
import com.example.demo.domain.batch.entity.BatchStatus;
import com.example.demo.domain.batch.entity.BatchStep;
import com.example.demo.domain.batch.exception.BatchHandler;
import com.example.demo.domain.batch.repository.BatchExecutionLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static com.example.demo.common.consts.StaticVariable.SEOUL_ZONE;

/**
 * 실행 이력을 바꾸는 트랜잭션 단위.
 *
 * 전부 REQUIRES_NEW다. 체인이 롤백돼도 FAILED 기록은 남아야 한다.
 * 실패가 함께 사라지면 이력을 남기는 의미가 없다.
 *
 * {@link BatchExecutionLogServiceImpl}에서 분리한 이유는 유니크 제약 위반을 재시도해야 하기 때문이다.
 * 제약 위반이 나면 그 트랜잭션은 rollback-only로 마킹돼 같은 트랜잭션 안에서는 재조회조차 못 한다.
 * 재시도가 새 트랜잭션이어야 하는데 자기 호출은 프록시를 타지 않으므로 빈을 나눈다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Transactional(propagation = Propagation.REQUIRES_NEW)
class BatchExecutionLogWriter {

    private final BatchExecutionLogRepository batchExecutionLogRepository;

    public BatchStartResult claim(LocalDate tradeDate, BatchStep step, boolean force) {
        LocalDateTime now = LocalDateTime.now(SEOUL_ZONE);

        Optional<BatchExecutionLog> found =
                batchExecutionLogRepository.findByTradeDateAndStepForUpdate(tradeDate, step);
        if (found.isEmpty()) {
            // 같은 순간에 다른 실행도 여기에 도달할 수 있다. 그 경우 유니크 제약이 한쪽을 튕겨내고,
            // 호출자가 재시도하면 상대가 만든 행을 보게 된다.
            BatchExecutionLog created =
                    batchExecutionLogRepository.save(BatchExecutionLog.start(tradeDate, step, now));
            return BatchStartResult.started(created.ref());
        }

        BatchExecutionLog existing = found.get();
        if (force) {
            log.warn("강제 재실행입니다. tradeDate={} step={} 이전상태={}", tradeDate, step, existing.getStatus());
            existing.restart(now);
            return BatchStartResult.started(existing.ref());
        }

        return switch (existing.getStatus()) {
            case SUCCESS -> {
                log.info("이미 성공한 단계라 건너뜁니다. tradeDate={} step={}", tradeDate, step);
                yield BatchStartResult.alreadyDone();
            }
            // 살아 있는 실행을 뺏으면 같은 단계가 두 번 돈다. 리포트 단계라면 GPT 비용이 두 배다.
            case RUNNING -> {
                if (isAlive(existing, now)) {
                    log.warn("다른 실행이 진행 중이라 중단합니다. tradeDate={} step={} startedAt={}",
                            tradeDate, step, existing.getStartedAt());
                    yield BatchStartResult.inProgress();
                }
                log.warn("죽은 실행으로 보고 인계합니다. tradeDate={} step={} startedAt={} attempt={}",
                        tradeDate, step, existing.getStartedAt(), existing.getAttempt());
                existing.restart(now);
                yield BatchStartResult.started(existing.ref());
            }
            case FAILED, SKIPPED -> {
                existing.restart(now);
                yield BatchStartResult.started(existing.ref());
            }
        };
    }

    public void succeed(BatchExecutionRef ref, String message) {
        finish(ref, message, (found, now) -> found.succeed(now, message));
    }

    public void fail(BatchExecutionRef ref, String message) {
        finish(ref, message, (found, now) -> found.fail(now, message));
    }

    /**
     * 이미 성공한 단계는 건드리지 않는다.
     * 성공 기록을 SKIPPED로 덮으면 그날 실제로 무슨 일이 있었는지 복구할 수 없고,
     * 이후 실행이 그 단계를 재실행 대상으로 오인한다.
     */
    public void markSkipped(LocalDate tradeDate, BatchStep step, String message) {
        LocalDateTime now = LocalDateTime.now(SEOUL_ZONE);

        Optional<BatchExecutionLog> found =
                batchExecutionLogRepository.findByTradeDateAndStepForUpdate(tradeDate, step);
        if (found.isEmpty()) {
            // claim과 같은 삽입 경합이 가능하다. 호출자가 재시도한다.
            BatchExecutionLog created =
                    batchExecutionLogRepository.save(BatchExecutionLog.start(tradeDate, step, now));
            created.skip(now, message);
            return;
        }

        BatchExecutionLog existing = found.get();
        if (existing.getStatus() == BatchStatus.SUCCESS) {
            log.warn("이미 성공한 단계라 스킵으로 덮어쓰지 않습니다. tradeDate={} step={} 스킵사유={}",
                    tradeDate, step, message);
            return;
        }

        existing.skip(now, message);
    }

    /**
     * 인계된 뒤 뒤늦게 끝난 이전 실행의 결과는 버린다.
     * 그대로 반영하면 지금 돌고 있는 실행이 SUCCESS/FAILED로 바뀌고,
     * FAILED가 되면 다음 실행이 재시작 대상으로 보고 같은 단계를 하나 더 띄운다.
     *
     * 잠금 조회다. 잠금 없이 읽으면 스냅샷으로 attempt를 확인하는 사이에 인계가 끝나,
     * 검사는 통과했는데 정작 갱신은 새 실행의 행에 적용될 수 있다.
     */
    private void finish(BatchExecutionRef ref, String message, FinishAction action) {
        BatchExecutionLog found = batchExecutionLogRepository.findByIdForUpdate(ref.executionId())
                .orElseThrow(BatchHandler::executionNotFound);

        if (!found.isCurrentAttempt(ref)) {
            log.warn("이미 다른 실행에 인계된 단계라 결과를 반영하지 않습니다. "
                            + "executionId={} 도착한attempt={} 현재attempt={} 결과={}",
                    ref.executionId(), ref.attempt(), found.getAttempt(), message);
            return;
        }

        action.apply(found, LocalDateTime.now(SEOUL_ZONE));
    }

    /**
     * 시작한 지 얼마 안 된 RUNNING은 아직 돌고 있는 실행으로 본다.
     * 기준 시간은 단계마다 다르다. {@link BatchStep}을 보라.
     *
     * 이 판정이 없으면 체인 도중 앱이 죽었을 때 RUNNING이 남아 그 거래일이 영구히 스킵된다.
     * 반대로 기준이 너무 짧으면 살아 있는 실행을 뺏어 같은 단계가 두 번 돈다.
     */
    private boolean isAlive(BatchExecutionLog running, LocalDateTime now) {
        return !running.getStartedAt().plus(running.getStep().getRunningTimeout()).isBefore(now);
    }

    @FunctionalInterface
    private interface FinishAction {
        void apply(BatchExecutionLog log, LocalDateTime now);
    }
}
