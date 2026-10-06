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

        Optional<BatchExecutionLog> existingLog =
                batchExecutionLogRepository.findByTradeDateAndStepForUpdate(tradeDate, step);
        if (existingLog.isEmpty()) {
            // 같은 순간에 다른 실행도 여기에 도달할 수 있다. 그 경우 유니크 제약이 한쪽을 튕겨내고,
            // 호출자가 재시도하면 상대가 만든 행을 보게 된다.
            BatchExecutionLog created =
                    batchExecutionLogRepository.save(BatchExecutionLog.start(tradeDate, step, now));
            return BatchStartResult.started(created.ref());
        }

        BatchExecutionLog execution = existingLog.get();

        // 살아 있는 실행은 누구도 뺏지 않는다. force도 예외가 아니다.
        // force의 뜻은 "이미 끝난 단계도 다시 실행한다"이지 "돌고 있는 걸 가로챈다"가 아니다.
        //
        // 단일 인스턴스에서는 큐 없는 실행기가 중복 제출을 앞에서 막아주지만,
        // 인스턴스가 둘 이상이거나 블루/그린 배포로 두 프로세스가 잠시 공존하면 여기까지 온다.
        // 그때 REPORT를 뺏으면 리포트 요청이 두 벌 나가 GPT 비용이 두 배가 된다.
        if (execution.getStatus() == BatchStatus.RUNNING && isAlive(execution, now)) {
            log.warn("다른 실행이 진행 중이라 중단합니다. tradeDate={} step={} startedAt={} force={}",
                    tradeDate, step, execution.getStartedAt(), force);
            return BatchStartResult.inProgress();
        }

        if (force) {
            log.warn("강제 재실행입니다. tradeDate={} step={} 이전상태={}", tradeDate, step, execution.getStatus());
            execution.restart(now);
            return BatchStartResult.started(execution.ref());
        }

        return switch (execution.getStatus()) {
            case SUCCESS -> {
                log.info("이미 성공한 단계라 건너뜁니다. tradeDate={} step={}", tradeDate, step);
                yield BatchStartResult.alreadyDone();
            }
            // 위에서 살아 있는 RUNNING을 걸러냈으므로 여기 오는 건 죽은 실행뿐이다.
            case RUNNING -> {
                log.warn("죽은 실행으로 보고 인계합니다. tradeDate={} step={} startedAt={} attempt={}",
                        tradeDate, step, execution.getStartedAt(), execution.getAttempt());
                execution.restart(now);
                yield BatchStartResult.started(execution.ref());
            }
            case FAILED, SKIPPED -> {
                execution.restart(now);
                yield BatchStartResult.started(execution.ref());
            }
        };
    }

    public void succeed(BatchExecutionRef ref, String message) {
        finish(ref, message, (execution, finishedAt) -> execution.succeed(finishedAt, message));
    }

    public void fail(BatchExecutionRef ref, String message) {
        finish(ref, message, (execution, finishedAt) -> execution.fail(finishedAt, message));
    }

    /**
     * 이미 끝났거나 지금 돌고 있는 단계는 건드리지 않는다.
     *
     * SUCCESS를 덮으면 그날 실제로 무슨 일이 있었는지 복구할 수 없고,
     * 이후 실행이 그 단계를 재실행 대상으로 오인한다.
     *
     * 살아 있는 RUNNING을 덮는 건 더 나쁘다. 그 실행은 계속 돌고 있는데 행은 SKIPPED가 되므로,
     * 다음 실행이 재시작 대상으로 보고 같은 단계를 하나 더 띄운다.
     * claim이 RUNNING을 isAlive로 보호하는 것과 같은 기준을 쓴다.
     */
    public void skip(LocalDate tradeDate, BatchStep step, String message) {
        LocalDateTime now = LocalDateTime.now(SEOUL_ZONE);

        Optional<BatchExecutionLog> existingLog =
                batchExecutionLogRepository.findByTradeDateAndStepForUpdate(tradeDate, step);
        if (existingLog.isEmpty()) {
            // claim과 같은 삽입 경합이 가능하다. 호출자가 재시도한다.
            BatchExecutionLog created =
                    batchExecutionLogRepository.save(BatchExecutionLog.start(tradeDate, step, now));
            created.skip(now, message);
            return;
        }

        BatchExecutionLog execution = existingLog.get();
        if (execution.getStatus() == BatchStatus.SUCCESS) {
            log.warn("이미 성공한 단계라 스킵으로 덮어쓰지 않습니다. tradeDate={} step={} 스킵사유={}",
                    tradeDate, step, message);
            return;
        }
        if (execution.getStatus() == BatchStatus.RUNNING && isAlive(execution, now)) {
            log.warn("다른 실행이 진행 중이라 스킵으로 덮어쓰지 않습니다. tradeDate={} step={} startedAt={} 스킵사유={}",
                    tradeDate, step, execution.getStartedAt(), message);
            return;
        }

        execution.skip(now, message);
    }

    /**
     * 도착 확인을 성공으로 닫는다. 콜백은 실행 참조를 모르므로 거래일로 찾는다.
     *
     * FAILED에서도 SUCCESS로 간다. 스위퍼가 미도착으로 판정한 직후 마지막 콜백이 도착하는 경합이
     * 실제로 가능하고, 그때 최종 상태는 "리포트가 다 있다"여야 한다.
     * 운영자가 미도착 리포트를 수동으로 다시 만들었을 때 FAILED를 되돌리는 경로이기도 하다.
     */
    public void completeVerification(LocalDate tradeDate, String message) {
        Optional<BatchExecutionLog> existingLog =
                batchExecutionLogRepository.findByTradeDateAndStepForUpdate(tradeDate, BatchStep.VERIFY);
        if (existingLog.isEmpty()) {
            // 그날 배치가 리포트를 요청한 적이 없다. 단발 리포트 콜백이 여기로 온다.
            return;
        }

        // 계약에 적힌 두 전이만 허용한다. RUNNING -> SUCCESS, FAILED -> SUCCESS.
        // "SUCCESS가 아니면 전부"로 두면 나중에 VERIFY가 다른 상태를 갖게 됐을 때 조용히 딸려온다.
        BatchExecutionLog verification = existingLog.get();
        if (verification.getStatus() != BatchStatus.RUNNING
                && verification.getStatus() != BatchStatus.FAILED) {
            log.info("도착 확인이 닫을 수 있는 상태가 아닙니다. tradeDate={} 상태={}",
                    tradeDate, verification.getStatus());
            return;
        }

        log.info("도착 확인을 성공으로 닫습니다. tradeDate={} 이전상태={}",
                tradeDate, verification.getStatus());
        verification.succeed(LocalDateTime.now(SEOUL_ZONE), message);
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
        BatchExecutionLog execution = batchExecutionLogRepository.findByIdForUpdate(ref.executionId())
                .orElseThrow(BatchHandler::executionNotFound);

        if (!execution.isCurrentAttempt(ref)) {
            log.warn("이미 다른 실행에 인계된 단계라 결과를 반영하지 않습니다. "
                            + "executionId={} 도착한attempt={} 현재attempt={} 결과={}",
                    ref.executionId(), ref.attempt(), execution.getAttempt(), message);
            return;
        }

        // 종료 전이는 RUNNING에서만 일어난다.
        // 스위퍼와 콜백이 겹치면 이미 끝난 단계에 다른 결과를 덮어쓸 수 있다.
        if (execution.getStatus() != BatchStatus.RUNNING) {
            log.warn("이미 끝난 단계라 결과를 반영하지 않습니다. executionId={} 현재상태={} 결과={}",
                    ref.executionId(), execution.getStatus(), message);
            return;
        }

        action.apply(execution, LocalDateTime.now(SEOUL_ZONE));
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

    /** 종료 상태로의 전이. 파라미터를 log로 두면 Slf4j 필드를 가린다. */
    @FunctionalInterface
    private interface FinishAction {
        void apply(BatchExecutionLog execution, LocalDateTime finishedAt);
    }
}
