package com.example.demo.domain.prediction.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PredictionDuration {

    ONE_DAY("1일 뒤", 1, 5),
    THREE_DAY("3일 뒤", 3, 4),
    FIVE_DAY("5일 뒤", 5, 3),
    ONE_WEEK("1주일", 7, 2),
    TWO_WEEK("2주일", 14, 1);

    private final String description;
    private final int days;   // 제출일 기준 캘린더 일수
    private final int weight; // duration 짧을수록 가중치 ↑
}
