package com.example.demo.domain.batch.entity;

/** 일일 배치 한 회차의 결말. */
public enum DailyBatchOutcome {
    /**
     * 체인이 끝까지 갔다. 각 단계가 실행됐는지 건너뛰어졌는지는 실행 이력을 봐야 안다.
     * 휴장일도 여기로 온다. 직전 거래일의 단계가 전부 ALREADY_DONE으로 지나가기 때문이다.
     */
    COMPLETED,
    /**
     * 중간에 멈췄다. message에 이유가 있다.
     * failedStep은 어느 단계에서 멈췄는지인데, 거래일조차 확보하지 못해
     * 단계를 시작하기 전에 멈춘 경우에는 tradeDate와 함께 비어 있다.
     */
    ABORTED
}
