package com.example.demo.domain.batch.entity;

public enum BatchStatus {
    /** 실행 중. startedAt이 오래됐으면 죽은 실행으로 보고 인계한다. */
    RUNNING,
    SUCCESS,
    FAILED,
    /** 휴장일 등으로 실행할 필요가 없어 건너뛴 상태. 실패와 구분해야 한다. */
    SKIPPED
}
