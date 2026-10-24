package com.example.demo.domain.notification.service;

import com.example.demo.domain.notification.entity.Notification;
import com.example.demo.domain.notification.entity.NotificationType;
import com.example.demo.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/** 알림 내역 저장과 커밋 후 FCM 발송 이벤트 생성을 한 곳에서 처리한다. */
@Service
@RequiredArgsConstructor
public class NotificationDispatchService {
    private final NotificationCommandService notificationCommandService;
    private final ApplicationEventPublisher eventPublisher;

    public Notification create(User user, Long sourceId, NotificationType type,
                               String title, String message) {
        Notification saved = notificationCommandService.createNotification(Notification.builder()
                .user(user)
                .sourceId(sourceId)
                .notificationType(type)
                .message(message)
                .isRead(false)
                .build());
        eventPublisher.publishEvent(new NotificationPublishedEvent(
                user.getId(), saved.getId(), sourceId, type, title, message));
        return saved;
    }
}
