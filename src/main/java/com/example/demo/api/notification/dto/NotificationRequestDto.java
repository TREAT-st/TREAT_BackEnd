package com.example.demo.api.notification.dto;

import com.example.demo.domain.notification.entity.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class NotificationRequestDto {

    @Getter
    @NoArgsConstructor
    public static class SendNotificationRequest {
        @NotNull
        private Long sourceId;
        @NotNull
        private NotificationType notificationType;
        @NotBlank
        private String message;
    }
}
