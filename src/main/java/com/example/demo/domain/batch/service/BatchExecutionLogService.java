package com.example.demo.domain.batch.service;

import com.example.demo.domain.batch.entity.BatchExecutionLog;
import com.example.demo.domain.batch.entity.BatchExecutionRef;
import com.example.demo.domain.batch.entity.BatchStartResult;
import com.example.demo.domain.batch.entity.BatchStep;

import java.time.LocalDate;
import java.util.List;

public interface BatchExecutionLogService {

    /**
     * 단계 실행 권한을 얻는다.
     * 결과에 따라 실행/건너뛰기/중단이 갈리므로 status를 반드시 분기해야 한다.
     *
     * @param force 이미 성공한 단계도 다시 실행한다. 운영자가 강제로 되돌릴 때만 쓴다.
     */
    BatchStartResult tryStart(LocalDate tradeDate, BatchStep step, boolean force);

    /** ref가 가리키는 시도가 아직 살아 있을 때만 반영된다. 인계된 뒤라면 무시한다. */
    void succeed(BatchExecutionRef ref, String message);

    /** ref가 가리키는 시도가 아직 살아 있을 때만 반영된다. 인계된 뒤라면 무시한다. */
    void fail(BatchExecutionRef ref, String message);

    /**
     * 실행할 대상이 없어 건너뛰었음을 남긴다. 실패와 구분해야 하는 정상 상황이다.
     * 이미 성공한 단계는 덮어쓰지 않는다.
     */
    void skip(LocalDate tradeDate, BatchStep step, String message);

    List<BatchExecutionLog> getByTradeDate(LocalDate tradeDate);
}
