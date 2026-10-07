package com.example.demo.api.userDevice.dto;

import com.example.demo.domain.userDevice.entity.DevicePlatform;
import jakarta.validation.constraints.*;

public class UserDeviceRequestDto {
    public record RegisterDeviceRequest(
            @NotBlank @Size(max = 512) @Pattern(regexp = "\\S+") String fcmToken,
            @NotNull DevicePlatform platform) {
    }
}
