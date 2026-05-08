package com.example.demo.api.notification.mapper;

import com.example.demo.api.notification.dto.NotificationRequestDto.*;
import com.example.demo.api.notification.dto.NotificationResponseDto.*;
import com.example.demo.domain.notification.entity.Notification;
import com.example.demo.domain.user.entity.User;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.stream.Collectors;

public class NotificationConverter {

    public static Notification toNotification(User user, NotificationRequest request) {
        return Notification.builder()
                .user(user)
                .sourceId(request.getSourceId())
                .notificationType(request.getNotificationType())
                .message(request.getMessage())
                .isRead(false)
                .build();
    }

    public static NotificationResponse toNotificationResponse(Notification notification) {
        return NotificationResponse.builder()
                .notificationId(notification.getId())
                .userId(notification.getUser().getId())
                .sourceId(notification.getSourceId())
                .notificationType(notification.getNotificationType())
                .message(notification.getMessage())
                .isRead(notification.getIsRead())
                .build();
    }

    public static NotificationPageResponse toNotificationPageResponse(Page<Notification> notificationPage) {
        List<NotificationResponse> content = notificationPage.getContent().stream()
                .map(NotificationConverter::toNotificationResponse)
                .collect(Collectors.toList());

        return NotificationPageResponse.builder()
                .content(content)
                .page(notificationPage.getNumber())
                .totalPages(notificationPage.getTotalPages())
                .totalElements(notificationPage.getTotalElements())
                .hasNext(notificationPage.hasNext())
                .build();
    }
}
