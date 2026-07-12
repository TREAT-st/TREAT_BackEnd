package com.example.demo.domain.prediction.service;

import com.example.demo.domain.prediction.entity.Prediction;
import com.example.demo.domain.prediction.exception.PredictionErrorStatus;
import com.example.demo.domain.prediction.exception.PredictionHandler;
import com.example.demo.domain.prediction.repository.PredictionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PredictionQueryServiceImpl implements PredictionQueryService {

    private final PredictionRepository predictionRepository;

    @Override
    public Prediction getById(Long predictionId) {
        return predictionRepository.findById(predictionId)
                .orElseThrow(() -> new PredictionHandler(PredictionErrorStatus.PREDICTION_NOT_FOUND));
    }

    @Override
    public Prediction getByIdAndUser(Long predictionId, Long userId) {
        Prediction prediction = getById(predictionId);
        if (!prediction.getUser().getId().equals(userId)) {
            throw new PredictionHandler(PredictionErrorStatus.PREDICTION_FORBIDDEN);
        }
        return prediction;
    }
}
