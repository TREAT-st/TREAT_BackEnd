package com.example.demo.domain.prediction.service;

import com.example.demo.domain.prediction.entity.Prediction;
import com.example.demo.domain.prediction.entity.PredictionStatus;

public interface PredictionCommandService {
    Prediction createPrediction(Prediction prediction);
    Prediction grade(Prediction prediction, PredictionStatus status);
}