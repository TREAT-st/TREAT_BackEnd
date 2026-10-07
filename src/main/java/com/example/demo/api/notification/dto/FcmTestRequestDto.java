package com.example.demo.api.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Map;

public class FcmTestRequestDto {

    public record FcmTestSendRequest(
            @NotBlank
            @Size(max = 100)
            String title,

            @NotBlank
            @Size(max = 500)
            String body,

            Map<String, String> data
    ) {
    }
}
