package com.example.demo.api.prediction.service;

import com.example.demo.api.prediction.dto.PredictionResponseDto.GradePredictionResponse;
import com.example.demo.api.prediction.mapper.PredictionConverter;
import com.example.demo.domain.prediction.entity.Prediction;
import com.example.demo.domain.prediction.entity.PredictionStatus;
import com.example.demo.domain.prediction.exception.PredictionHandler;
import com.example.demo.domain.prediction.port.KisPricePort;
import com.example.demo.domain.prediction.service.PredictionCommandService;
import com.example.demo.domain.prediction.service.PredictionQueryService;
import com.example.demo.domain.userPortfolio.service.UserPortfolioCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class PredictionGradingService {
    private final PredictionQueryService predictionQueryService;
    private final PredictionCommandService predictionCommandService;
    private final UserPortfolioCommandService userPortfolioCommandService;
    private final KisPricePort kisPricePort;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public GradePredictionResponse grade(Long predictionId) {
        Prediction prediction = predictionQueryService.getPredictionById(predictionId);
        if (prediction.getStatus() != PredictionStatus.PENDING) {
            throw PredictionHandler.ALREADY_GRADED;
        }
        BigDecimal currentPrice = kisPricePort.getCurrentPrice(prediction.getStock().getStockCode());
        double changeRate = currentPrice
                .subtract(prediction.getBasePrice())
                .divide(prediction.getBasePrice(), 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();

        boolean isHit = prediction.getTarget().isHit(changeRate);
        PredictionStatus resultStatus = isHit ? PredictionStatus.CORRECT : PredictionStatus.WRONG;

        Prediction graded = predictionCommandService.grade(prediction, resultStatus);
        userPortfolioCommandService.recordGradingResult(
                prediction.getUser().getId(), isHit, prediction.getEarnablePoints());

        return PredictionConverter.toGradeResponse(graded, changeRate);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(Long predictionId) {
        Prediction prediction = predictionQueryService.getPredictionById(predictionId);
        if (prediction.getStatus() == PredictionStatus.PENDING) {
            prediction.markGradingFailed();
        }
    }
}
