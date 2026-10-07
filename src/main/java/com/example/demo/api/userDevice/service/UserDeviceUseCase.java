package com.example.demo.api.userDevice.service;

import com.example.demo.api.userDevice.dto.UserDeviceRequestDto.RegisterDeviceRequest;
import com.example.demo.api.userDevice.dto.UserDeviceResponseDto.DeviceResponse;
import com.example.demo.common.annotation.UseCase;
import com.example.demo.domain.user.entity.User;
import com.example.demo.domain.userDevice.service.UserDeviceCommandService;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class UserDeviceUseCase {
    private final UserDeviceCommandService commandService;

    public DeviceResponse register(User user, String installationId, RegisterDeviceRequest request) {
        return DeviceResponse.from(commandService.register(user, installationId, request.fcmToken(), request.platform()));
    }

    public void unregister(Long userId, String installationId) {
        commandService.unregister(userId, installationId);
    }
}
