package com.example.demo.domain.batch.entity;

/** 한 거래일의 한 단계가 지금 어떤 상태인지. 시작 판정인 {@link BatchStartDecision}과 다르다. */
public enum BatchStatus {
    /**
     * 실행 중. startedAt이 오래됐으면 죽은 실행으로 보고 인계한다.
     * 얼마나 지나야 오래된 것인지는 단계마다 다르다. {@link BatchStep}의 runningTimeout을 보라.
     */
    RUNNING,
    SUCCESS,
    FAILED,
    /**
     * 실행할 대상이 없어 건너뛴 상태. 실패와 구분해야 한다.
     * 지금은 탐지된 종목이 0건이라 리포트를 만들 게 없을 때만 생긴다.
     *
     * 휴장일은 여기 해당하지 않는다. 그때 KRX가 주는 거래일은 이미 처리가 끝난 직전 거래일이라
     * 그 날짜로 기록을 남기면 지난 성공 이력을 덮는다. 그래서 아예 기록하지 않는다.
     */
    SKIPPED
}
