package com.example.demo.api.prediction.service;

import com.example.demo.api.prediction.dto.PredictionRequestDto.SubmitPredictionRequest;
import com.example.demo.api.prediction.dto.PredictionResponseDto.*;
import com.example.demo.api.prediction.mapper.PredictionConverter;
import com.example.demo.common.annotation.UseCase;
import com.example.demo.domain.prediction.entity.Prediction;
import com.example.demo.domain.prediction.entity.PredictionDuration;
import com.example.demo.domain.prediction.exception.PredictionHandler;
import com.example.demo.domain.prediction.service.PredictionCommandService;
import com.example.demo.domain.prediction.service.PredictionQueryService;
import com.example.demo.domain.stock.entity.Stock;
import com.example.demo.domain.stock.service.StockQueryService;
import com.example.demo.domain.user.entity.User;
import com.example.demo.domain.userPortfolio.service.UserPortfolioCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.math.BigDecimal;
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
    private final PredictionGradingService predictionGradingService;

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
        return predictionGradingService.grade(predictionId);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public SchedulerRunResponse gradeMaturedPredictions() {
        List<Prediction> matured = predictionQueryService.getMaturedPendingPredictions();
        int gradedCount = 0;
        int failedCount = 0;

        for (Prediction prediction : matured) {
            try {
                predictionGradingService.grade(prediction.getId());
                gradedCount++;
            } catch (Exception e) {
                log.error("[예측 채점 스케줄러] 예측 ID={} 채점 실패: {}", prediction.getId(), e.getMessage());
                failedCount++;
                try {
                    predictionGradingService.markFailed(prediction.getId());
                } catch (Exception statusException) {
                    log.error("[예측 채점 스케줄러] 예측 ID={} 실패 상태 저장 실패", prediction.getId(), statusException);
                }
            }
        }

        log.info("[예측 채점 스케줄러] 완료 — 성공={}, 실패={}", gradedCount, failedCount);
        return SchedulerRunResponse.builder()
                .gradedCount(gradedCount)
                .failedCount(failedCount)
                .build();
    }

    private LocalDateTime calcMaturityAt(PredictionDuration duration) {
        LocalDate base = LocalDate.now(ZoneId.of("Asia/Seoul")).plusDays(duration.getDays());
        DayOfWeek dow = base.getDayOfWeek();
        if (dow == DayOfWeek.SATURDAY) {
            base = base.plusDays(2); // 토 → 월
        } else if (dow == DayOfWeek.SUNDAY) {
            base = base.plusDays(1); // 일 → 월
        }
        return base.atTime(15, 30);
    }
}