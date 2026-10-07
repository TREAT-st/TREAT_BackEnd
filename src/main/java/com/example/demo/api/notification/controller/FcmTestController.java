package com.example.demo.api.notification.controller;

import com.example.demo.api.common.dto.ApiResponseDto;
import com.example.demo.api.notification.dto.FcmTestRequestDto.FcmTestSendRequest;
import com.example.demo.common.annotation.AuthUser;
import com.example.demo.domain.notification.service.FcmSendResult;
import com.example.demo.domain.notification.service.FcmService;
import com.example.demo.domain.user.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "[개발용] FCM 테스트 API")
@RestController
@RequestMapping("/api/v1/test/fcm")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "firebase.enabled", havingValue = "true")
public class FcmTestController {

    private final FcmService fcmService;

    @Operation(
            summary = "로그인 사용자의 기기로 테스트 알림 발송",
            description = "JWT로 인증된 사용자에게 등록된 모든 기기로 FCM 알림을 발송합니다."
    )
    @PostMapping("/send")
    public ApiResponseDto<FcmSendResult> sendToMyDevices(
            @AuthUser User user,
            @RequestBody @Valid FcmTestSendRequest request) {
        // 요청에서 사용자 ID를 받지 않아 다른 사용자의 기기로 테스트 알림을 보낼 수 없습니다.
        FcmSendResult result = fcmService.sendToUser(
                user.getId(),
                request.title(),
                request.body(),
                request.data()
        );

        return ApiResponseDto.onSuccess(result);
    }
}
