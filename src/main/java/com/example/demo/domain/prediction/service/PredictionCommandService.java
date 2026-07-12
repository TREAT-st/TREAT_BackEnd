package com.example.demo.domain.prediction.service;

import com.example.demo.domain.prediction.entity.Prediction;

import java.math.BigDecimal;
import java.util.List;

public interface PredictionCommandService {

    // 예측 저장
    Prediction save(Prediction prediction);

    // 단건 채점 (수동/스케줄러 공용). 반환: 지급 포인트
    int grade(Prediction prediction, BigDecimal maturityPrice);

    // 만기 도래 PENDING 예측 일괄 조회 (스케줄러용)
    List<Prediction> findMaturedPendings();
}
