package com.example.demo.domain.batch.entity;

import com.example.demo.domain.model.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "batch_execution_log",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_batch_execution_trade_date_step",
                columnNames = {"trade_date", "step"}),
        indexes = @Index(name = "idx_batch_execution_trade_date", columnList = "trade_date")
)
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class BatchExecutionLog extends BaseTimeEntity {

    /** message 컬럼 길이. 스택트레이스가 그대로 들어오면 넘치므로 잘라서 담는다. */
    private static final int MESSAGE_MAX_LENGTH = 1000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "batch_execution_log_id")
    private Long id;

    /**
     * 배치 기준일. 정상 실행에서는 KRX가 알려준 거래일이다.
     *
     * 거래일을 확보하지 못한 경우(휴장일 판정, KRX 장애)만 예외다. 그때는 남길 거래일이 없어
     * 서울 기준 실행일을 키로 쓴다. 그 날짜는 거래일이 아니므로 진짜 단계 기록과 부딪히지 않는다.
     */
    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "step", nullable = false, length = 20)
    private BatchStep step;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BatchStatus status;

    /**
     * 시도 회차. 재시작할 때마다 오른다.
     * 재시작이 같은 행을 재사용하므로, 이 값이 없으면 인계된 뒤 뒤늦게 끝난 이전 실행이
     * 지금 돌고 있는 실행의 상태를 덮어쓴다.
     */
    @Column(name = "attempt", nullable = false)
    private int attempt;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "message", length = MESSAGE_MAX_LENGTH)
    private String message;

    public static BatchExecutionLog start(LocalDate tradeDate, BatchStep step, LocalDateTime startedAt) {
        return BatchExecutionLog.builder()
                .tradeDate(tradeDate)
                .step(step)
                .status(BatchStatus.RUNNING)
                .attempt(1)
                .startedAt(startedAt)
                .build();
    }

    public void restart(LocalDateTime startedAt) {
        this.attempt++;
        this.status = BatchStatus.RUNNING;
        this.startedAt = startedAt;
        this.finishedAt = null;
        this.message = null;
    }

    /** 지금 살아 있는 시도가 맞는지. 아니면 이미 다른 실행에 인계된 것이다. */
    public boolean isCurrentAttempt(BatchExecutionRef ref) {
        return this.attempt == ref.attempt();
    }

    public BatchExecutionRef ref() {
        return new BatchExecutionRef(id, attempt);
    }

    public void succeed(LocalDateTime finishedAt, String message) {
        finish(BatchStatus.SUCCESS, finishedAt, message);
    }

    public void fail(LocalDateTime finishedAt, String message) {
        finish(BatchStatus.FAILED, finishedAt, message);
    }

    public void skip(LocalDateTime finishedAt, String message) {
        finish(BatchStatus.SKIPPED, finishedAt, message);
    }

    private void finish(BatchStatus status, LocalDateTime finishedAt, String message) {
        this.status = status;
        this.finishedAt = finishedAt;
        this.message = truncateMessage(message);
    }

    private static String truncateMessage(String message) {
        if (message == null || message.length() <= MESSAGE_MAX_LENGTH) {
            return message;
        }
        return message.substring(0, MESSAGE_MAX_LENGTH);
    }
}
