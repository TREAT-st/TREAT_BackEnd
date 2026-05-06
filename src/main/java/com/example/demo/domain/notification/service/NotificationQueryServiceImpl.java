package com.example.demo.domain.notification.service;

import com.example.demo.domain.notification.entity.Notification;
import com.example.demo.domain.notification.exception.NotificationHandler;
import com.example.demo.domain.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class NotificationQueryServiceImpl implements NotificationQueryService {
    private final NotificationRepository notificationRepository;

    @Override
    public Notification getNotificationById(Long notificationId) {
        return notificationRepository.findById(notificationId)
                .orElseThrow(() -> NotificationHandler.NOT_FOUND);
    }

    @Override
    public Page<Notification> getNotificationListByPage(Long userId, Pageable pageable) {
        return notificationRepository.findAllByUserId(userId, pageable);
    }
}
