package com.example.demo.domain.prediction.service;

import com.example.demo.domain.prediction.entity.Prediction;
import com.example.demo.domain.prediction.entity.PredictionStatus;
import com.example.demo.domain.prediction.repository.PredictionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class PredictionCommandServiceImpl implements PredictionCommandService {

    private final PredictionRepository predictionRepository;

    @Override
    public Prediction createPrediction(Prediction prediction) {
        return predictionRepository.save(prediction);
    }

    @Override
    public Prediction grade(Prediction prediction, PredictionStatus status) {
        prediction.grade(status);
        return predictionRepository.save(prediction);
    }
}