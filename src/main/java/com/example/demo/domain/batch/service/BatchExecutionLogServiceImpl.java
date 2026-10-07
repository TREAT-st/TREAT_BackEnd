package com.example.demo.domain.batch.service;

import com.example.demo.domain.batch.entity.BatchExecutionLog;
import com.example.demo.domain.batch.entity.BatchExecutionRef;
import com.example.demo.domain.batch.entity.BatchStartResult;
import com.example.demo.domain.batch.entity.BatchStep;
import com.example.demo.domain.batch.repository.BatchExecutionLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * 실행 이력 API.
 *
 * 쓰기는 전부 {@link BatchExecutionLogWriter}가 자기 트랜잭션에서 처리하고,
 * 여기서는 트랜잭션을 열지 않은 채 유니크 제약 경합만 흡수한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BatchExecutionLogServiceImpl implements BatchExecutionLogService {

    private final BatchExecutionLogRepository batchExecutionLogRepository;
    private final BatchExecutionLogWriter batchExecutionLogWriter;

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public BatchStartResult tryStart(LocalDate tradeDate, BatchStep step, boolean force) {
        return retryOnConcurrentInsert(tradeDate, step,
                () -> batchExecutionLogWriter.claim(tradeDate, step, force));
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void succeed(BatchExecutionRef ref, String message) {
        batchExecutionLogWriter.succeed(ref, message);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void fail(BatchExecutionRef ref, String message) {
        batchExecutionLogWriter.fail(ref, message);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void completeVerification(LocalDate tradeDate, String message) {
        batchExecutionLogWriter.completeVerification(tradeDate, message);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void skip(LocalDate tradeDate, BatchStep step, String message) {
        retryOnConcurrentInsert(tradeDate, step, () -> {
            batchExecutionLogWriter.skip(tradeDate, step, message);
            return null;
        });
    }

    /**
     * step이 문자열로 저장돼 DB 정렬로는 실행 순서가 나오지 않는다.
     * 한 거래일에 최대 4건이라 메모리에서 enum 선언 순서로 정렬한다.
     */
    @Override
    @Transactional(readOnly = true)
    public List<BatchExecutionLog> getByTradeDate(LocalDate tradeDate) {
        return batchExecutionLogRepository.findAllByTradeDate(tradeDate).stream()
                .sorted(Comparator.comparing(BatchExecutionLog::getStep))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<LocalDate> getLatestTradeDate() {
        return batchExecutionLogRepository.findFirstByOrderByStartedAtDescIdDesc()
                .map(BatchExecutionLog::getTradeDate);
    }

    /**
     * 행이 없을 때는 조회와 삽입 사이에 틈이 있어 두 실행이 동시에 "없다"고 판단할 수 있다.
     * 아직 없는 행은 잠글 수도 없으므로 유니크 제약이 한쪽을 튕겨내는 것으로 막고,
     * 튕긴 쪽은 새 트랜잭션에서 다시 판단한다. 그때는 상대가 만든 행이 보인다.
     *
     * 트랜잭션 밖이어야 한다. 제약 위반이 난 트랜잭션은 rollback-only로 마킹돼
     * 그 안에서는 재조회조차 못 한다.
     */
    private <T> T retryOnConcurrentInsert(LocalDate tradeDate, BatchStep step, Supplier<T> writeOperation) {
        try {
            return writeOperation.get();
        } catch (DataIntegrityViolationException e) {
            log.info("동시 삽입이 감지돼 다시 판단합니다. tradeDate={} step={}", tradeDate, step);
            return writeOperation.get();
        }
    }
}
