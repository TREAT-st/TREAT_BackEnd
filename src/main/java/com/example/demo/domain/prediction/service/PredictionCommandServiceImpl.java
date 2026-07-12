package com.example.demo.domain.prediction.service;

import com.example.demo.domain.prediction.entity.Prediction;
import com.example.demo.domain.prediction.entity.PredictionStatus;
import com.example.demo.domain.prediction.exception.PredictionErrorStatus;
import com.example.demo.domain.prediction.exception.PredictionHandler;
import com.example.demo.domain.prediction.repository.PredictionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PredictionCommandServiceImpl implements PredictionCommandService {

    private final PredictionRepository predictionRepository;

    @Override
    public Prediction save(Prediction prediction) {
        return predictionRepository.save(prediction);
    }

    @Override
    public int grade(Prediction prediction, BigDecimal maturityPrice) {
        if (!prediction.isPending()) {
            throw new PredictionHandler(PredictionErrorStatus.PREDICTION_ALREADY_GRADED);
        }
        if (maturityPrice == null || maturityPrice.signum() <= 0) {
            throw new PredictionHandler(PredictionErrorStatus.INVALID_PRICE);
        }
        // 영속 상태 엔티티라 dirty checking 으로 반영됨
        return prediction.grade(maturityPrice);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Prediction> findMaturedPendings() {
        return predictionRepository.findByStatusAndMaturityAtBefore(
                PredictionStatus.PENDING, LocalDateTime.now());
    }
}
