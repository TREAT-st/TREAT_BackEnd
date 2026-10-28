package com.example.demo.api.prediction.service;

import com.example.demo.domain.prediction.entity.PredictionStatus;
import com.example.demo.domain.prediction.entity.PredictionTarget;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** 가격 변화율을 계산하고 예측의 적중 여부를 판정한다. */
@Component
public class PredictionGrader {

    public GradeDecision decide(BigDecimal basePrice, BigDecimal closePrice, PredictionTarget target) {
        double changeRate = closePrice
                .subtract(basePrice)
                .divide(basePrice, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
        boolean correct = target.isHit(changeRate);
        return new GradeDecision(changeRate, correct ? PredictionStatus.CORRECT : PredictionStatus.WRONG);
    }

    public record GradeDecision(double changeRate, PredictionStatus status) {
        public boolean isCorrect() {
            return status == PredictionStatus.CORRECT;
        }
    }
}
