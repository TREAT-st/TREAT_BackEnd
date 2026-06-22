package com.example.demo.api.user.dto;

import com.example.demo.domain.user.entity.Gender;
import com.example.demo.domain.user.entity.SocialProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

public class UserRequestDto {
    @Getter
    @NoArgsConstructor
    public static class RegisterUserRequest {
        @NotBlank
        private String name;
        @NotBlank
        private String kakaoEmail;
        private String profileImg;
        @NotNull
        private LocalDate birthDate;
        @NotNull
        private Gender gender;
        @NotBlank
        private String nickname;
        private String accountNumber;
        @NotNull
        private SocialProvider provider;
        @NotBlank
        private String providerUserId;
    }

    @Getter
    @NoArgsConstructor
    public static class UpdateUserRequest {
        private String nickname;
        private String profileImg;
        private String accountNumber;
    }
}
