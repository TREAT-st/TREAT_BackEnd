package com.example.demo.domain.batch.repository;

import com.example.demo.domain.batch.entity.BatchExecutionLog;
import com.example.demo.domain.batch.entity.BatchStatus;
import com.example.demo.domain.batch.entity.BatchStep;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BatchExecutionLogRepository extends JpaRepository<BatchExecutionLog, Long> {

    /**
     * 읽기 전용 조회. 상태를 바꿀 목적으로 쓰면 안 된다.
     * 잠금이 없어 읽은 값이 곧바로 낡을 수 있다. 변경에는 아래 ForUpdate 조회를 쓴다.
     */
    Optional<BatchExecutionLog> findByTradeDateAndStep(LocalDate tradeDate, BatchStep step);

    /**
     * 상태를 바꿀 목적으로 읽을 때 쓴다.
     * 잠금이 없으면 두 실행이 같은 FAILED 행을 동시에 읽고 둘 다 재시작해버린다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM BatchExecutionLog b WHERE b.tradeDate = :tradeDate AND b.step = :step")
    Optional<BatchExecutionLog> findByTradeDateAndStepForUpdate(@Param("tradeDate") LocalDate tradeDate,
                                                                @Param("step") BatchStep step);

    /**
     * 완료 처리용 조회. attempt 검사와 상태 변경이 한 잠금 안에서 일어나야 한다.
     * 잠금 없이 읽으면 스냅샷으로 attempt를 확인한 뒤, 그 사이 인계가 끝난 행에 결과를 쓰게 된다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM BatchExecutionLog b WHERE b.id = :id")
    Optional<BatchExecutionLog> findByIdForUpdate(@Param("id") Long id);

    List<BatchExecutionLog> findAllByTradeDate(LocalDate tradeDate);

    /** 도착 확인 스위퍼가 그레이스를 넘긴 단계를 찾을 때 쓴다. */
    List<BatchExecutionLog> findAllByStepAndStatusAndStartedAtBefore(
            BatchStep step, BatchStatus status, LocalDateTime startedAt);
}
