package com.example.demo.api.userDevice.dto;

import com.example.demo.domain.userDevice.entity.*;
import java.time.LocalDateTime;

public class UserDeviceResponseDto {
    public record DeviceResponse(Long deviceId, String installationId, DevicePlatform platform,
                                 LocalDateTime updatedAt) {
        public static DeviceResponse from(UserDevice device) {
            return new DeviceResponse(device.getId(), device.getInstallationId(),
                    device.getPlatform(), device.getUpdatedAt());
        }
    }
}
