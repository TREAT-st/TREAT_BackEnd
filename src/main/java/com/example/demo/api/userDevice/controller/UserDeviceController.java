package com.example.demo.api.userDevice.controller;

import com.example.demo.api.common.dto.ApiResponseDto;
import com.example.demo.api.userDevice.dto.UserDeviceRequestDto.RegisterDeviceRequest;
import com.example.demo.api.userDevice.dto.UserDeviceResponseDto.DeviceResponse;
import com.example.demo.api.userDevice.service.UserDeviceUseCase;
import com.example.demo.common.annotation.AuthUser;
import com.example.demo.domain.user.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "사용자 기기 API")
@RestController
@RequestMapping("/api/v1/users/me/devices")
@RequiredArgsConstructor
public class UserDeviceController {
    private final UserDeviceUseCase useCase;

    @Operation(summary = "기기 토큰 등록·갱신", description = "같은 설치 ID를 재등록하면 현재 로그인 사용자에게 연결합니다.")
    @PatchMapping("/{installationId}")
    public ApiResponseDto<DeviceResponse> register(
            @AuthUser User user,
            @PathVariable @Size(min = 1, max = 128) @Pattern(regexp = "[A-Za-z0-9_-]+") String installationId,
            @RequestBody @Valid RegisterDeviceRequest request) {
        return ApiResponseDto.onSuccess(useCase.register(user, installationId, request));
    }

    @Operation(summary = "기기 연결 해제", description = "현재 사용자의 기기만 해제합니다. 이미 해제됐거나 다른 사용자 소유이면 변경 없이 성공합니다.")
    @DeleteMapping("/{installationId}")
    public ApiResponseDto<Void> unregister(
            @AuthUser User user,
            @PathVariable @Size(min = 1, max = 128) @Pattern(regexp = "[A-Za-z0-9_-]+") String installationId) {
        useCase.unregister(user.getId(), installationId);
        return ApiResponseDto.onSuccess(null);
    }
}
