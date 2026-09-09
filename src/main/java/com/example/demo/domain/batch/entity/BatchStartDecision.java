package com.example.demo.domain.batch.entity;

/**
 * 단계를 시작해도 되는지에 대한 판정. 호출자가 취해야 할 행동이 셋 다 다르다.
 *
 * 저장된 단계 상태인 {@link BatchStatus}와 다른 개념이다.
 * 그쪽은 지금 어떤 상태인지를, 이쪽은 시작해도 되는지를 말한다.
 */
public enum BatchStartDecision {
    /** 이 단계를 실행한다. */
    STARTED,
    /** 이미 성공한 단계다. 건너뛰고 다음 단계로 진행한다. */
    ALREADY_DONE,
    /** 다른 실행이 이 단계를 진행 중이다. 체인 전체를 중단한다. */
    IN_PROGRESS
}
