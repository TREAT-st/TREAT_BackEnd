package com.example.demo.api.notification.service;

import com.example.demo.api.notification.dto.NotificationRequestDto.*;
import com.example.demo.api.notification.dto.NotificationResponseDto.NotificationPageResponse;
import com.example.demo.api.notification.dto.NotificationResponseDto.NotificationResponse;
import com.example.demo.api.notification.mapper.NotificationConverter;
import com.example.demo.common.annotation.UseCase;
import com.example.demo.domain.notification.entity.Notification;
import com.example.demo.domain.notification.exception.NotificationHandler;
import com.example.demo.domain.notification.service.NotificationCommandService;
import com.example.demo.domain.notification.service.NotificationQueryService;
import com.example.demo.domain.notification.entity.NotificationType;
import com.example.demo.domain.prediction.repository.PredictionRepository;
import com.example.demo.domain.volatility.repository.VolatilityRepository;
import com.example.demo.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@Transactional
@RequiredArgsConstructor
public class NotificationUseCase {
    private final NotificationQueryService notificationQueryService;
    private final NotificationCommandService notificationCommandService;
    private final PredictionRepository predictionRepository;
    private final VolatilityRepository volatilityRepository;

    //  TODO: 내부 처리해야 함. 테스트 용으로 만든 것.
    public Notification sendNotification(User user, NotificationRequest request) {
        Notification notification = NotificationConverter.toNotification(user, request);

        return notificationCommandService.createNotification(notification);
    }

    public NotificationResponse sendNotificationResponse(User user, NotificationRequest request) {
        return toResponse(sendNotification(user, request));
    }

    public Long readNotification(Long userId, Long notificationId) {
        Notification notification = notificationQueryService.getNotificationById(notificationId);
        if (!notification.getUser().getId().equals(userId))
            throw NotificationHandler.FORBIDDEN;
        notification.markAsRead();

        return notification.getId();
    }

    @Transactional(readOnly = true)
    public Page<Notification> getNotificationList(Long userId, Pageable pageable) {
        return notificationQueryService.getNotificationListByPage(userId, pageable);
    }

    @Transactional(readOnly = true)
    public NotificationPageResponse getNotificationListResponse(Long userId, Pageable pageable) {
        Page<Notification> notificationPage = getNotificationList(userId, pageable);
        return NotificationPageResponse.builder()
                .content(notificationPage.getContent().stream().map(this::toResponse).toList())
                .page(notificationPage.getNumber())
                .totalPages(notificationPage.getTotalPages())
                .totalElements(notificationPage.getTotalElements())
                .hasNext(notificationPage.hasNext())
                .build();
    }

    public Long deleteNotification(Long userId, Long notificationId) {
        Notification notification = notificationQueryService.getNotificationById(notificationId);
        if (!notification.getUser().getId().equals(userId))
            throw NotificationHandler.FORBIDDEN;

        return notificationCommandService.deleteNotification(notification);
    }

    private NotificationResponse toResponse(Notification notification) {
        StockInfo stockInfo = resolveStockInfo(notification);
        return NotificationConverter.toNotificationResponse(
                notification, stockInfo.stockCode(), stockInfo.stockName());
    }

    private StockInfo resolveStockInfo(Notification notification) {
        if (notification.getNotificationType() == NotificationType.RESULT) {
            return predictionRepository.findById(notification.getSourceId())
                    .map(prediction -> new StockInfo(
                            prediction.getStock().getStockCode(),
                            prediction.getStock().getStockName()))
                    .orElseGet(StockInfo::empty);
        }

        if (notification.getNotificationType() == NotificationType.VOLATILITY_REPORT) {
            return volatilityRepository.findById(notification.getSourceId())
                    .map(volatility -> new StockInfo(
                            volatility.getStockCode(), volatility.getStockName()))
                    .orElseGet(StockInfo::empty);
        }

        return StockInfo.empty();
    }

    private record StockInfo(String stockCode, String stockName) {
        private static StockInfo empty() {
            return new StockInfo(null, null);
        }
    }
}
