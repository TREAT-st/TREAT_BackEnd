package com.example.demo.domain.prediction.service;

import com.example.demo.domain.prediction.entity.Prediction;
import com.example.demo.domain.prediction.entity.PredictionStatus;
import com.example.demo.domain.prediction.exception.PredictionHandler;
import com.example.demo.domain.prediction.repository.PredictionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PredictionQueryServiceImpl implements PredictionQueryService {

    private final PredictionRepository predictionRepository;

    @Override
    public Prediction getPredictionById(Long predictionId) {
        return predictionRepository.findById(predictionId)
                .orElseThrow(() -> PredictionHandler.NOT_FOUND);
    }

    @Override
    public List<Prediction> getMaturedPendingPredictions() {
        return predictionRepository.findAllByStatusAndMaturityAtBefore(
                PredictionStatus.PENDING, LocalDateTime.now());
    }
}