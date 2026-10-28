package com.example.demo.api.prediction.service;

import com.example.demo.api.prediction.dto.PredictionGradingResultDto;
import com.example.demo.domain.notification.entity.NotificationType;
import com.example.demo.domain.notification.service.NotificationDispatchService;
import com.example.demo.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 예측 채점 결과를 사용자 알림과 FCM 발송 이벤트로 변환한다. */
@Service
@RequiredArgsConstructor
public class PredictionResultNotificationService {
    private final NotificationDispatchService notificationDispatchService;

    public void notify(User user, PredictionGradingResultDto result) {
        String message = result.isCorrect()
                ? result.stockName() + " 예측이 적중했어요. 포인트를 확인해 보세요."
                : result.stockName() + " 예측 결과가 나왔어요. 결과를 확인해 보세요.";
        notificationDispatchService.create(user, result.predictionId(), NotificationType.RESULT,
                "주식 예측 결과", message);
    }
}
