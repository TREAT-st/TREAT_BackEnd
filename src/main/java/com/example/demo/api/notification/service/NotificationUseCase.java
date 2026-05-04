package com.example.demo.api.notification.service;

import com.example.demo.api.notification.dto.NotificationRequestDto.*;
import com.example.demo.api.notification.mapper.NotificationConverter;
import com.example.demo.common.annotation.UseCase;
import com.example.demo.domain.notification.entity.Notification;
import com.example.demo.domain.notification.exception.NotificationHandler;
import com.example.demo.domain.notification.service.NotificationCommandService;
import com.example.demo.domain.notification.service.NotificationQueryService;
import com.example.demo.domain.user.entity.User;
import com.example.demo.domain.user.service.UserQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class NotificationUseCase {
    private final NotificationQueryService notificationQueryService;
    private final NotificationCommandService notificationCommandService;

    //  TODO: 내부 처리해야 함. 테스트 용으로 만든 것.
    @Transactional
    public Notification sendNotification(User user, NotificationRequest request) {
        Notification notification = NotificationConverter.toNotification(user, request);

        return notificationCommandService.createNotification(notification);
    }

    public Page<Notification> getNotificationList(Long userId, Pageable pageable) {
        return notificationQueryService.getNotificationListByPage(userId, pageable);
    }

    @Transactional
    public Long readNotification(Long userId, Long notificationId) {
        Notification notification = notificationQueryService.getNotificationById(notificationId);
        if (!notification.getUser().getId().equals(userId))
            throw NotificationHandler.FORBIDDEN;
        notification.markAsRead();

        return notification.getId();
    }

    @Transactional
    public Long deleteNotification(Long userId, Long notificationId) {
        Notification notification = notificationQueryService.getNotificationById(notificationId);
        if (!notification.getUser().getId().equals(userId))
            throw NotificationHandler.FORBIDDEN;

        return notificationCommandService.deleteNotification(notification);
    }
}
