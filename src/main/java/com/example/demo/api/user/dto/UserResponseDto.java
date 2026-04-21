package com.example.demo.api.user.dto;

import com.example.demo.domain.user.entity.Role;
import com.example.demo.domain.user.entity.UserStatus;
import lombok.Builder;
import lombok.Getter;

public class UserResponseDto {

    @Getter
    @Builder
    public static class RegisterUserResponse {
        private Long userId;
        private String nickname;
        private Role role;
        private UserStatus status;
    }
}
