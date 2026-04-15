package com.example.demo.domain.notification.service;

import com.example.demo.domain.notification.entity.Notification;

public interface NotificationCommandService {
    Notification createNotification(Notification notification);
    Long deleteNotification(Long notificationId);
}
