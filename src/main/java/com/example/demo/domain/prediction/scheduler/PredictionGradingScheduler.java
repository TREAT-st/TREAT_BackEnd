package com.example.demo.domain.prediction.scheduler;

import com.example.demo.api.prediction.service.PredictionUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PredictionGradingScheduler {

    private final PredictionUseCase predictionUseCase;

    /**
     * 평일 16:30(KST) 자동 채점 실행
     * 만기 도래한 PENDING 예측을 일괄 조회 → KIS 종가 조회 → 채점 → 포인트 지급
     */
    @Scheduled(cron = "0 30 16 * * MON-FRI", zone = "Asia/Seoul")
    public void gradeMaturedPredictions() {
        log.info("[예측 채점 스케줄러] 자동 실행 시작");
        predictionUseCase.gradeMaturedPredictions();
    }
}