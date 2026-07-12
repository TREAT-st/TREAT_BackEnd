package com.example.demo.api.prediction.service;

import com.example.demo.api.prediction.dto.PredictionRequestDto.CreatePrediction;
import com.example.demo.api.prediction.dto.PredictionResponseDto.CreatePredictionResult;
import com.example.demo.api.prediction.dto.PredictionResponseDto.PredictionResult;
import com.example.demo.api.prediction.mapper.PredictionConverter;
import com.example.demo.domain.prediction.entity.Prediction;
import com.example.demo.domain.prediction.service.PredictionCommandService;
import com.example.demo.domain.prediction.service.PredictionQueryService;
import com.example.demo.domain.stock.entity.Stock;
import com.example.demo.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Transactional
public class PredictionUseCase {

    private final PredictionCommandService predictionCommandService;
    private final PredictionQueryService predictionQueryService;

    // 기존 서비스에 붙이는 얇은 어댑터 3개
    private final StockQueryPort stockQueryPort;
    private final KisPricePort kisPricePort;
    private final UserPointPort userPointPort;

    /** 예측 제출: 기준가 확보 → 저장 → 획득가능 포인트 응답 */
    public CreatePredictionResult createPrediction(User user, CreatePrediction req) {
        Stock stock = stockQueryPort.getByStockCode(req.getStockCode());

        // 제출 시점 기준가 (KIS 현재가). KIS 막혀있으면 KisPricePort에서 고정값 반환 중.
        BigDecimal basePrice = kisPricePort.getCurrentPrice(req.getStockCode());

        Prediction prediction = PredictionConverter.toPrediction(
                user, stock, req.getDuration(), req.getTarget(), basePrice);

        Prediction saved = predictionCommandService.save(prediction);
        return PredictionConverter.toCreateResult(saved);
    }

    /** 결과 조회 */
    @Transactional(readOnly = true)
    public PredictionResult getResult(User user, Long predictionId) {
        Prediction prediction = predictionQueryService.getByIdAndUser(predictionId, user.getId());
        return PredictionConverter.toResult(prediction);
    }

    /** 데모/테스트용 수동 채점: 종가를 직접 받아 즉시 채점 + 포인트 지급 */
    public PredictionResult gradeManually(User user, Long predictionId, BigDecimal maturityPrice) {
        Prediction prediction = predictionQueryService.getByIdAndUser(predictionId, user.getId());
        int earned = predictionCommandService.grade(prediction, maturityPrice);
        if (earned > 0) {
            userPointPort.addPoint(prediction.getUser(), earned);
        }
        return PredictionConverter.toResult(prediction);
    }
}
