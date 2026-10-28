package com.example.demo.domain.notification.repository;

import com.example.demo.domain.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Page<Notification> findAllByUserId(Long userId, Pageable pageable);
    boolean existsByUserIdAndSourceIdAndNotificationType(Long userId, Long sourceId,
                                                         com.example.demo.domain.notification.entity.NotificationType notificationType);
}
