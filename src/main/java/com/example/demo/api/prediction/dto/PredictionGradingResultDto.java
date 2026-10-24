package com.example.demo.api.prediction.dto;

import com.example.demo.domain.prediction.entity.PredictionStatus;

/** 채점 이후 포인트·알림 처리에서 공유하는 내부 결과 DTO다. */
public record PredictionGradingResultDto(
        Long predictionId,
        Long userId,
        String stockName,
        PredictionStatus status,
        double changeRate,
        long earnablePoints) {

    public boolean isCorrect() {
        return status == PredictionStatus.CORRECT;
    }
}
