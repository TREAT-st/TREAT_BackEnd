package com.example.demo.domain.prediction.service;

import com.example.demo.domain.prediction.entity.Prediction;

import java.util.List;

public interface PredictionQueryService {
    Prediction getPredictionById(Long predictionId);
    List<Prediction> getMaturedPendingPredictions();
}