package com.example.demo.api.user.dto;

import com.example.demo.domain.user.entity.Gender;
import com.example.demo.domain.user.entity.Role;
import com.example.demo.domain.user.entity.UserStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

public class UserResponseDto {

    @Getter
    @Builder
    public static class RegisterUserResponse {
        private Long userId;
        private String nickname;
        private Role role;
        private UserStatus status;
    }

    @Getter
    @Builder
    public static class UpdateUserResponse {
        private Long userId;
        private String name;
        private String nickname;
        private String profileImg;
        private LocalDate birthDate;
        private Gender gender;
        private String accountNumber;
    }
}
