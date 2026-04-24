package com.example.demo.api.notification.controller;

import com.example.demo.api.common.dto.ApiResponseDto;
import com.example.demo.api.notification.dto.NotificationRequestDto.*;
import com.example.demo.api.notification.dto.NotificationResponseDto.*;
import com.example.demo.api.notification.mapper.NotificationConverter;
import com.example.demo.api.notification.service.NotificationUseCase;
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

@Tag(name = "[사용자 알림 페이지] 사용자 알림 API")
@RestController
@RequestMapping("/api/v1/users/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationUseCase notificationUseCase;

    // TODO: @AuthUser 적용하기
    @Operation(summary = "알림 발송", description = "사용자에게 알림을 발송합니다.")
    @PostMapping
    public ApiResponseDto<NotificationResponse> sendNotification(
            User user,
            @RequestBody @Valid NotificationRequest request) {
        Notification notification = notificationUseCase.sendNotification(user, request);
        return ApiResponseDto.onSuccess(NotificationConverter.toNotificationResponse(notification));
    }

    // TODO: @AuthUser 적용하기
    @Operation(summary = "user의 notification 조회", description = "user의 notification을 PageNation으로 조회합니다.")
    @GetMapping
    public ApiResponseDto<NotificationPageResponse> getNotification(
            User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        Pageable pageable = PageRequest.of(page, pageSize, Sort.by(Sort.Order.desc("createdDate")));
        Page<Notification> notificationPage = notificationUseCase.getNotificationList(user.getId(), pageable);
        return ApiResponseDto.onSuccess(NotificationConverter.toNotificationPageResponse(notificationPage));
    }

    @Operation(summary = "사용자의 알림을 '읽음' 상태로 만듭니다.")
    @PatchMapping("/{notificationId}")
    public ApiResponseDto<Long> updateNotification(User user, @PathVariable Long notificationId) {
        return ApiResponseDto.onSuccess(notificationUseCase.readNotification(user, notificationId));
    }

    // TODO: @AuthUser 적용하기
    @Operation(summary = "user의 notification을 삭제", description = "user의 notification을 삭제합니다.")
    @DeleteMapping("/{notificationId}")
    public ApiResponseDto<Long> deleteNotification(User user, @PathVariable Long notificationId) {
        return ApiResponseDto.onSuccess(notificationUseCase.deleteNotification(user, notificationId));
    }
}
