package com.example.demo.api.prediction.scheduler;

import com.example.demo.api.prediction.service.KisPricePort;
import com.example.demo.api.prediction.service.UserPointPort;
import com.example.demo.domain.prediction.entity.Prediction;
import com.example.demo.domain.prediction.service.PredictionCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 만기 도래 예측을 자동으로 채점하는 스케줄러.
 * 평일 장 마감 후 16:30(KST)에 실행.
 * KIS API가 연동되면 KisPricePort.getClosePriceAt()이 실제 종가를 반환합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PredictionGradingScheduler {

    private final PredictionCommandService predictionCommandService;
    private final KisPricePort kisPricePort;
    private final UserPointPort userPointPort;

    /**
     * 평일 16:30 실행 (KST = UTC+9, 서버가 UTC라면 "0 30 7 * * MON-FRI").
     * 서버 TimeZone이 Asia/Seoul이면 "0 30 16 * * MON-FRI" 사용.
     */
    @Scheduled(cron = "0 30 16 * * MON-FRI", zone = "Asia/Seoul")
    @Transactional
    public void gradeMaturedPredictions() {
        List<Prediction> targets = predictionCommandService.findMaturedPendings();
        if (targets.isEmpty()) {
            log.info("[PredictionScheduler] 채점 대상 없음.");
            return;
        }

        log.info("[PredictionScheduler] 채점 시작. 대상 수={}", targets.size());
        int successCount = 0;
        int failCount = 0;

        for (Prediction prediction : targets) {
            try {
                String stockCode = prediction.getStock().getStockCode();
                BigDecimal maturityPrice = kisPricePort.getClosePriceAt(
                        stockCode, prediction.getMaturityAt().toLocalDate());

                int earned = predictionCommandService.grade(prediction, maturityPrice);
                if (earned > 0) {
                    userPointPort.addPoint(prediction.getUser(), earned);
                }
                successCount++;
                log.debug("[PredictionScheduler] 채점 완료. predictionId={}, status={}, earnedPoint={}",
                        prediction.getId(), prediction.getStatus(), earned);

            } catch (Exception e) {
                failCount++;
                log.error("[PredictionScheduler] 채점 실패. predictionId={}, error={}",
                        prediction.getId(), e.getMessage(), e);
            }
        }

        log.info("[PredictionScheduler] 채점 완료. 성공={}, 실패={}", successCount, failCount);
    }
}