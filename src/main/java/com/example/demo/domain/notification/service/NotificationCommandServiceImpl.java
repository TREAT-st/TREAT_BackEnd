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

    /*  TODO: 삭제를 요청한 notification이 요청한 user의 것이 맞는지 확인해야함.
         -> notificationUseCase를 만들어서 처리 예정.
    */
    @Override
    public Long deleteNotification(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> NotificationHandler.NOT_FOUND);
        notificationRepository.delete(notification);
        return notification.getId();
    }
}
