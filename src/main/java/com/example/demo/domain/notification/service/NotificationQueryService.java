package com.example.demo.domain.notification.service;

import com.example.demo.domain.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationQueryService {
    Page<Notification> getNotificationListByPage(Long userId, Pageable pageable);
}
