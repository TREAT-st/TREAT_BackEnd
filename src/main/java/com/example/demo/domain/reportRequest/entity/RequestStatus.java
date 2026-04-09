package com.example.demo.domain.reportRequest.entity;

public enum RequestStatus {
    PENDING,        // 대기
    PROCESSING,     // 처리 중
    SUCCESS,        // 성공
    FAILED,         // 실패 (재시도 가능)
    RETRYING        // 재시도 중
}
