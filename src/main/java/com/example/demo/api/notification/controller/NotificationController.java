package com.example.demo.api.notification.controller;

import com.example.demo.api.common.dto.ApiResponseDto;
import com.example.demo.api.notification.dto.NotificationRequestDto.*;
import com.example.demo.api.notification.dto.NotificationResponseDto.*;
import com.example.demo.api.notification.mapper.NotificationConverter;
import com.example.demo.api.notification.service.NotificationUseCase;
import com.example.demo.common.annotation.AuthUser;
import com.example.demo.domain.notification.entity.Notification;
import com.example.demo.domain.user.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import static com.example.demo.common.consts.StaticVariable.CREATED_DATE;

@Tag(name = "[사용자 알림 페이지] 사용자 알림 API")
@RestController
@RequestMapping("/api/v1/users/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationUseCase notificationUseCase;

    @Operation(summary = "알림 발송", description = "사용자에게 알림을 발송합니다.")
    @PostMapping
    public ApiResponseDto<NotificationResponse> sendNotification(
            @AuthUser User user,
            @RequestBody @Valid NotificationRequest request) {
        Notification notification = notificationUseCase.sendNotification(user, request);

        return ApiResponseDto.onSuccess(NotificationConverter.toNotificationResponse(notification));
    }

    @Operation(summary = "user의 notification 조회", description = "user의 notification을 PageNation으로 조회합니다.")
    @GetMapping
    public ApiResponseDto<NotificationPageResponse> getNotification(
            @AuthUser User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        Pageable pageable = PageRequest.of(page, pageSize, Sort.by(Sort.Order.desc(CREATED_DATE)));
        Page<Notification> notificationPage = notificationUseCase.getNotificationList(user.getId(), pageable);

        return ApiResponseDto.onSuccess(NotificationConverter.toNotificationPageResponse(notificationPage));
    }

    @Operation(summary = "사용자의 알림을 '읽음' 상태로 만듭니다.")
    @PatchMapping("/{notificationId}")
    public ApiResponseDto<Long> updateNotification(@AuthUser User user, @PathVariable Long notificationId) {
        return ApiResponseDto.onSuccess(notificationUseCase.readNotification(user.getId(), notificationId));
    }

    @Operation(summary = "user의 notification을 삭제", description = "user의 notification을 삭제합니다.")
    @DeleteMapping("/{notificationId}")
    public ApiResponseDto<Long> deleteNotification(@AuthUser User user, @PathVariable Long notificationId) {
        return ApiResponseDto.onSuccess(notificationUseCase.deleteNotification(user.getId(), notificationId));
    }
}
