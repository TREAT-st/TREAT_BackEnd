package com.example.demo.domain.prediction.service;

import com.example.demo.domain.prediction.entity.Prediction;

public interface PredictionQueryService {

    // 결과 조회 (본인 검증 포함)
    Prediction getByIdAndUser(Long predictionId, Long userId);

    Prediction getById(Long predictionId);

    long countByUserId(Long userId);
}
