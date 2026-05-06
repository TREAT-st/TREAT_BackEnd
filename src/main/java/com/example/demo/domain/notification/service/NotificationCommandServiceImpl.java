package com.example.demo.domain.notification.service;

import com.example.demo.domain.notification.entity.Notification;
import com.example.demo.domain.notification.exception.NotificationHandler;
import com.example.demo.domain.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class NotificationCommandServiceImpl implements NotificationCommandService {
    private final NotificationRepository notificationRepository;

    @Override
    public Notification createNotification(Notification notification) {
        return notificationRepository.save(notification);
    }

    @Override
    public Long readNotification(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> NotificationHandler.NOT_FOUND);

        notification.markAsRead();

        return notification.getId();
    }

    @Override
    public Long deleteNotification(Notification notification) {
        notificationRepository.delete(notification);
        return notification.getId();
    }
}
