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
}
