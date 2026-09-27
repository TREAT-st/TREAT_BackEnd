package com.example.demo.domain.prediction.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PredictionDuration {
    ONE_DAY(1, 10),
    THREE_DAY(3, 8),
    FIVE_DAY(5, 6),
    ONE_WEEK(7, 5),
    TWO_WEEK(14, 3);

    private final int days;
    private final int pointWeight;
}