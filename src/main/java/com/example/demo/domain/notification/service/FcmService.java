package com.example.demo.domain.notification.service;

import com.example.demo.domain.userDevice.service.UserDevicePushService;
import com.example.demo.domain.userDevice.service.UserDevicePushService.PushTarget;
import com.google.firebase.messaging.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "firebase.enabled", havingValue = "true")
public class FcmService {
    private final FirebaseMessaging firebaseMessaging;
    private final UserDevicePushService devicePushService;

    /**
     * 사용자의 모든 등록 기기에 같은 알림을 발송합니다.
     * data에는 notificationType, sourceId 등 앱 화면 이동에 필요한 문자열을 전달합니다.
     * 호출자는 채점/리포트 결과를 커밋한 뒤 호출해야 롤백된 결과의 알림이 발송되지 않습니다.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public FcmSendResult sendToUser(Long userId, String title, String body, Map<String, String> data) {
        Assert.notNull(userId, "userId is required");
        Assert.hasText(title, "title is required");
        Assert.hasText(body, "body is required");
        Map<String, String> payload = data == null ? Map.of() : Map.copyOf(data);
        Notification notification = Notification.builder().setTitle(title).setBody(body).build();
        int successCount = 0;
        int failureCount = 0;

        for (PushTarget target : devicePushService.getTargets(userId)) {
            try {
                Message message = Message.builder()
                        .setToken(target.token())
                        .setNotification(notification)
                        .putAllData(payload)
                        .build();
                firebaseMessaging.send(message);
                successCount++;
            } catch (FirebaseMessagingException e) {
                failureCount++;
                // 오류 메시지에는 토큰 등이 포함될 수 있어 식별 ID와 오류 코드만 기록합니다.
                log.warn("[FCM] 발송 실패 userId={}, deviceId={}, code={}",
                        userId, target.deviceId(), e.getMessagingErrorCode());
                // INVALID_ARGUMENT는 메시지 형식 오류일 수도 있으므로 토큰을 삭제하지 않습니다.
                if (e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
                    removeExpiredToken(userId, target);
                }
            } catch (RuntimeException e) {
                failureCount++;
                log.warn("[FCM] 발송 실패 userId={}, deviceId={}, type={}",
                        userId, target.deviceId(), e.getClass().getSimpleName());
            }
        }
        return new FcmSendResult(successCount, failureCount);
    }

    private void removeExpiredToken(Long userId, PushTarget target) {
        try {
            devicePushService.removeExpiredToken(userId, target);
        } catch (RuntimeException e) {
            // 만료 토큰 정리에 실패해도 다음 기기로 계속 발송합니다.
            log.warn("[FCM] 만료 토큰 정리 실패 userId={}, deviceId={}, type={}",
                    userId, target.deviceId(), e.getClass().getSimpleName());
        }
    }
}
