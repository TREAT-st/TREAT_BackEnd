package com.example.demo.api.prediction.dto;

import com.example.demo.domain.prediction.entity.PredictionDuration;
import com.example.demo.domain.prediction.entity.PredictionStatus;
import com.example.demo.domain.prediction.entity.PredictionTarget;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PredictionResponseDto {

    @Getter
    @Builder
    public static class SubmitPredictionResponse {
        private Long predictionId;
        private String stockCode;
        private String stockName;
        private PredictionDuration duration;
        private PredictionTarget target;
        private BigDecimal basePrice;
        private LocalDateTime maturityAt;
        private Long earnablePoints;
    }

    @Getter
    @Builder
    public static class PredictionResultResponse {
        private Long predictionId;
        private String stockCode;
        private String stockName;
        private PredictionDuration duration;
        private PredictionTarget target;
        private PredictionStatus status;
        private BigDecimal basePrice;
        private LocalDateTime maturityAt;
        private Long earnablePoints;
        private LocalDateTime gradedAt;
        // TODO: 실제 결과 원인 데이터 연동 필요 (현재 더미 하드코딩)
        private String reason;
    }

    @Getter
    @Builder
    public static class GradePredictionResponse {
        private Long predictionId;
        private PredictionStatus status;
        private Double changeRate;
        private Long awardedPoints;
        private LocalDateTime gradedAt;
    }

    @Getter
    @Builder
    public static class SchedulerRunResponse {
        private int gradedCount;
        private int failedCount;
    }
}