package com.example.demo.api.prediction.mapper;

import com.example.demo.api.prediction.dto.PredictionResponseDto.*;
import com.example.demo.domain.prediction.entity.Prediction;
import com.example.demo.domain.prediction.entity.PredictionStatus;

public class PredictionConverter {

    public static SubmitPredictionResponse toSubmitResponse(Prediction prediction) {
        return SubmitPredictionResponse.builder()
                .predictionId(prediction.getId())
                .stockCode(prediction.getStock().getStockCode())
                .stockName(prediction.getStock().getStockName())
                .duration(prediction.getDuration())
                .target(prediction.getTarget())
                .basePrice(prediction.getBasePrice())
                .maturityAt(prediction.getMaturityAt())
                .earnablePoints(prediction.getEarnablePoints())
                .build();
    }

    public static PredictionResultResponse toResultResponse(Prediction prediction) {
        // TODO: reason 데이터는 현재 더미 하드코딩
        String reason = prediction.getStatus() == PredictionStatus.CORRECT
                ? "예측이 적중했습니다."
                : prediction.getStatus() == PredictionStatus.WRONG
                ? "예측이 빗나갔습니다."
                : prediction.getStatus() == PredictionStatus.FAILED
                ? "채점 처리에 실패했습니다."
                : "아직 채점 전입니다.";

        return PredictionResultResponse.builder()
                .predictionId(prediction.getId())
                .stockCode(prediction.getStock().getStockCode())
                .stockName(prediction.getStock().getStockName())
                .duration(prediction.getDuration())
                .target(prediction.getTarget())
                .status(prediction.getStatus())
                .basePrice(prediction.getBasePrice())
                .maturityAt(prediction.getMaturityAt())
                .earnablePoints(prediction.getEarnablePoints())
                .gradedAt(prediction.getGradedAt())
                .reason(reason)
                .build();
    }

    public static GradePredictionResponse toGradeResponse(Prediction prediction, double changeRate) {
        long awardedPoints = prediction.getStatus() == PredictionStatus.CORRECT
                ? prediction.getEarnablePoints() : 0L;

        return GradePredictionResponse.builder()
                .predictionId(prediction.getId())
                .status(prediction.getStatus())
                .changeRate(changeRate)
                .awardedPoints(awardedPoints)
                .gradedAt(prediction.getGradedAt())
                .build();
    }
}