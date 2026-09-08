package com.example.demo.domain.batch.entity;

/** 단계 실행 권한 요청의 결과. 호출자가 취해야 할 행동이 셋 다 다르다. */
public enum BatchStartStatus {
    /** 이 단계를 실행한다. */
    STARTED,
    /** 이미 성공한 단계다. 건너뛰고 다음 단계로 진행한다. */
    ALREADY_DONE,
    /** 다른 실행이 이 단계를 진행 중이다. 체인 전체를 중단한다. */
    IN_PROGRESS
}
