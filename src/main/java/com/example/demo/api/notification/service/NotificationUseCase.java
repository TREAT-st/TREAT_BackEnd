package com.example.demo.api.notification.service;

import com.example.demo.api.notification.dto.NotificationRequestDto.SendNotificationRequest;
import com.example.demo.api.notification.mapper.NotificationConverter;
import com.example.demo.common.annotation.UseCase;
import com.example.demo.domain.notification.entity.Notification;
import com.example.demo.domain.notification.exception.NotificationHandler;
import com.example.demo.domain.notification.service.NotificationCommandService;
import com.example.demo.domain.notification.service.NotificationQueryService;
import com.example.demo.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@Transactional
@RequiredArgsConstructor
public class NotificationUseCase {
    private final NotificationCommandService notificationCommandService;
    private final NotificationQueryService notificationQueryService;

    public Notification sendNotification(User user, SendNotificationRequest request) {
        Notification notification = NotificationConverter.toNotification(user, request);
        return notificationCommandService.createNotification(notification);
    }

    public Long deleteNotification(User user, Long notificationId) {
        Notification notification = notificationQueryService.getNotificationById(notificationId);

        if (!notification.getUser().getId().equals(user.getId())) {
            throw NotificationHandler.FORBIDDEN;
        }

        return notificationCommandService.deleteNotification(notificationId);
    }
}
