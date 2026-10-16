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
     * 탐지된 종목이 0건이라 만들 리포트가 없을 때의 REPORT에서 생긴다.
     *
     * 휴장일은 여기 해당하지 않는다. 휴장일에는 이미 처리한 직전 거래일이 다시 내려와
     * 단계들이 ALREADY_DONE으로 지나가므로 새 행 자체가 생기지 않는다.
     */
    SKIPPED
}
