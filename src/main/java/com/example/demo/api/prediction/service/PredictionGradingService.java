package com.example.demo.api.prediction.service;

import com.example.demo.api.prediction.dto.PredictionResponseDto.GradePredictionResponse;
import com.example.demo.api.prediction.dto.PredictionGradingResultDto;
import com.example.demo.api.prediction.mapper.PredictionConverter;
import com.example.demo.domain.prediction.entity.Prediction;
import com.example.demo.domain.prediction.entity.PredictionStatus;
import com.example.demo.domain.prediction.exception.PredictionHandler;
import com.example.demo.domain.prediction.port.CurrentPricePort;
import com.example.demo.domain.prediction.service.PredictionCommandService;
import com.example.demo.domain.prediction.service.PredictionQueryService;
import com.example.demo.domain.userPortfolio.service.UserPortfolioCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class PredictionGradingService {
    private final PredictionQueryService predictionQueryService;
    private final PredictionCommandService predictionCommandService;
    private final UserPortfolioCommandService userPortfolioCommandService;
    private final CurrentPricePort currentPricePort;
    private final PredictionGrader predictionGrader;
    private final PredictionResultNotificationService predictionResultNotificationService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public GradePredictionResponse grade(Long predictionId) {
        Prediction prediction = predictionQueryService.getPredictionById(predictionId);
        if (prediction.getStatus() != PredictionStatus.PENDING) {
            throw PredictionHandler.ALREADY_GRADED;
        }
        BigDecimal currentPrice = currentPricePort.getCurrentPrice(prediction.getStock().getStockCode());
        PredictionGrader.GradeDecision decision = predictionGrader.decide(
                prediction.getBasePrice(), currentPrice, prediction.getTarget());

        Prediction graded = predictionCommandService.grade(prediction, decision.status());
        userPortfolioCommandService.recordGradingResult(
                prediction.getUser().getId(), decision.isCorrect(), prediction.getEarnablePoints());

        PredictionGradingResultDto result = new PredictionGradingResultDto(
                graded.getId(), prediction.getUser().getId(), prediction.getStock().getStockName(),
                graded.getStatus(), decision.changeRate(), graded.getEarnablePoints());
        predictionResultNotificationService.notify(prediction.getUser(), result);

        return PredictionConverter.toGradeResponse(graded, result.changeRate());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(Long predictionId) {
        Prediction prediction = predictionQueryService.getPredictionById(predictionId);
        if (prediction.getStatus() == PredictionStatus.PENDING) {
            prediction.markGradingFailed();
        }
    }
}
