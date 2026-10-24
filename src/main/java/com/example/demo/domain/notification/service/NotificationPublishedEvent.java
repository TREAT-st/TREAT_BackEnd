package com.example.demo.domain.notification.service;

import com.example.demo.domain.notification.entity.NotificationType;

import java.util.Map;

/** DB 알림이 커밋된 뒤 푸시를 보낼 때 사용하는 내부 이벤트다. */
public record NotificationPublishedEvent(
        Long userId,
        Long notificationId,
        Long sourceId,
        NotificationType notificationType,
        String title,
        String message) {

    public Map<String, String> data() {
        return Map.of(
                "notificationId", String.valueOf(notificationId),
                "notificationType", notificationType.name(),
                "sourceId", String.valueOf(sourceId));
    }
}
