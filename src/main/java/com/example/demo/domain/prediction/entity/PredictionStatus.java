package com.example.demo.domain.prediction.entity;

public enum PredictionStatus {
    PENDING,  // 만기 전, 채점 대기
    CORRECT,  // 적중
    WRONG     // 실패
}
