package com.example.demo.domain.batch.service;

import com.example.demo.domain.batch.entity.BatchExecutionLog;
import com.example.demo.domain.batch.entity.BatchExecutionRef;
import com.example.demo.domain.batch.entity.BatchStartResult;
import com.example.demo.domain.batch.entity.BatchStep;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BatchExecutionLogService {

    /**
     * 단계 실행 권한을 얻는다.
     * 결과에 따라 실행/건너뛰기/중단이 갈리므로 decision을 반드시 분기해야 한다.
     *
     * @param force 이미 성공한 단계도 다시 실행한다. 운영자가 강제로 되돌릴 때만 쓴다.
     */
    BatchStartResult tryStart(LocalDate tradeDate, BatchStep step, boolean force);

    /** ref가 가리키는 시도가 아직 RUNNING일 때만 반영된다. 인계됐거나 이미 끝났으면 무시한다. */
    void succeed(BatchExecutionRef ref, String message);

    /** ref가 가리키는 시도가 아직 RUNNING일 때만 반영된다. 인계됐거나 이미 끝났으면 무시한다. */
    void fail(BatchExecutionRef ref, String message);

    /**
     * 도착 확인을 성공으로 닫는다. 콜백이 부르며, 콜백은 실행 참조를 모르므로 거래일로 찾는다.
     *
     * FAILED에서도 SUCCESS로 간다. 스위퍼가 미도착으로 판정한 직후 마지막 콜백이 도착하거나,
     * 운영자가 수동으로 리포트를 다시 만들어 채운 경우다.
     * "리포트가 다 있다"가 "그레이스 안에 못 왔다"보다 강한 사실이다.
     *
     * 그날 도착 확인이 열려 있지 않으면 아무것도 하지 않는다.
     * 단발 리포트 콜백이 배치 이력을 건드리지 않게 하는 것도 이 규칙이 맡는다.
     */
    void completeVerification(LocalDate tradeDate, String message);

    /**
     * 실행할 대상이 없어 건너뛰었음을 남긴다. 실패와 구분해야 하는 정상 상황이다.
     * 이미 성공한 단계는 덮어쓰지 않는다.
     */
    void skip(LocalDate tradeDate, BatchStep step, String message);

    List<BatchExecutionLog> getByTradeDate(LocalDate tradeDate);

    /**
     * 가장 최근에 시작된 배치의 거래일. 실행 이력이 하나도 없으면 비어 있다.
     *
     * 배치는 실행일이 아니라 직전 거래일을 처리하므로(화요일 새벽에 월요일 거래일),
     * "오늘"로 조회하면 방금 끝난 실행이 보이지 않는다. 날짜 없이 조회할 때 이 값을 쓴다.
     * 거래일을 확보하지 못한 시작 실패는 실행일 키로 남으므로, 그 경우엔 실행일이 나온다.
     */
    Optional<LocalDate> getLatestTradeDate();
}
