package com.example.demo.api.notification.dto;

import com.example.demo.domain.notification.entity.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

public class NotificationResponseDto {

    @Getter
    @Builder
    public static class NotificationResponse {
        private Long notificationId;
        private Long userId;
        private Long sourceId;
        private NotificationType notificationType;
        private String message;
        private Boolean isRead;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NotificationPageResponse {
        private List<NotificationResponse> content;
        private int page;
        private int totalPages;
        private long totalElements;
        private boolean hasNext;
    }
}
