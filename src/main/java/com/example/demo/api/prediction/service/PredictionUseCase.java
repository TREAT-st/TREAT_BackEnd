package com.example.demo.api.prediction.service;

import com.example.demo.api.prediction.dto.PredictionRequestDto.SubmitPredictionRequest;
import com.example.demo.api.prediction.dto.PredictionResponseDto.*;
import com.example.demo.api.prediction.mapper.PredictionConverter;
import com.example.demo.common.annotation.UseCase;
import com.example.demo.domain.prediction.entity.Prediction;
import com.example.demo.domain.prediction.entity.PredictionDuration;
import com.example.demo.domain.prediction.entity.PredictionStatus;
import com.example.demo.domain.prediction.exception.PredictionHandler;
import com.example.demo.domain.prediction.port.KisPricePort;
import com.example.demo.domain.prediction.service.PredictionCommandService;
import com.example.demo.domain.prediction.service.PredictionQueryService;
import com.example.demo.domain.stock.entity.Stock;
import com.example.demo.domain.stock.service.StockQueryService;
import com.example.demo.domain.user.entity.User;
import com.example.demo.domain.userPortfolio.service.UserPortfolioCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@UseCase
@Transactional
@RequiredArgsConstructor
public class PredictionUseCase {

    private final PredictionCommandService predictionCommandService;
    private final PredictionQueryService predictionQueryService;
    private final UserPortfolioCommandService userPortfolioCommandService;
    private final StockQueryService stockQueryService;
    private final KisPricePort kisPricePort;

    public SubmitPredictionResponse submitPrediction(User user, SubmitPredictionRequest request) {
        Stock stock = stockQueryService.getStockByCode(request.getStockCode());
        BigDecimal basePrice = stock.getClosePrice();
        if (basePrice == null) {
            throw PredictionHandler.STOCK_PRICE_UNAVAILABLE;
        }
        LocalDateTime maturityAt = calcMaturityAt(request.getDuration());
        long earnablePoints = (long) request.getDuration().getPointWeight() * request.getTarget().getPointWeight();

        Prediction prediction = Prediction.builder()
                .user(user)
                .stock(stock)
                .duration(request.getDuration())
                .target(request.getTarget())
                .basePrice(basePrice)
                .maturityAt(maturityAt)
                .earnablePoints(earnablePoints)
                .build();

        Prediction saved = predictionCommandService.createPrediction(prediction);
        userPortfolioCommandService.recordNewPrediction(user.getId());

        return PredictionConverter.toSubmitResponse(saved);
    }

    @Transactional(readOnly = true)
    public PredictionResultResponse getPredictionResult(Long userId, Long predictionId) {
        Prediction prediction = predictionQueryService.getPredictionById(predictionId);
        if (!prediction.getUser().getId().equals(userId)) {
            throw PredictionHandler.FORBIDDEN;
        }
        return PredictionConverter.toResultResponse(prediction);
    }

    public GradePredictionResponse manualGrade(Long predictionId) {
        Prediction prediction = predictionQueryService.getPredictionById(predictionId);
        if (prediction.getStatus() != PredictionStatus.PENDING) {
            throw PredictionHandler.ALREADY_GRADED;
        }
        return gradeInternal(prediction);
    }

    public SchedulerRunResponse gradeMaturedPredictions() {
        List<Prediction> matured = predictionQueryService.getMaturedPendingPredictions();
        int gradedCount = 0;
        int failedCount = 0;

        for (Prediction prediction : matured) {
            try {
                gradeInternal(prediction);
                gradedCount++;
            } catch (Exception e) {
                log.error("[예측 채점 스케줄러] 예측 ID={} 채점 실패: {}", prediction.getId(), e.getMessage());
                failedCount++;
            }
        }

        log.info("[예측 채점 스케줄러] 완료 — 성공={}, 실패={}", gradedCount, failedCount);
        return SchedulerRunResponse.builder()
                .gradedCount(gradedCount)
                .failedCount(failedCount)
                .build();
    }

    private GradePredictionResponse gradeInternal(Prediction prediction) {
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

    private LocalDateTime calcMaturityAt(PredictionDuration duration) {
        LocalDate base = LocalDate.now(ZoneId.of("Asia/Seoul")).plusDays(duration.getDays());
        DayOfWeek dow = base.getDayOfWeek();
        if (dow == DayOfWeek.SATURDAY) {
            base = base.minusDays(1);
        } else if (dow == DayOfWeek.SUNDAY) {
            base = base.minusDays(2);
        }
        return base.atTime(15, 30);
    }
}