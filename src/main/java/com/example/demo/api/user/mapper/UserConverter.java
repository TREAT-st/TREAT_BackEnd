package com.example.demo.api.user.mapper;

import com.example.demo.api.user.dto.UserRequestDto;
import com.example.demo.api.user.dto.UserResponseDto;
import com.example.demo.domain.user.entity.Role;
import com.example.demo.domain.user.entity.User;
import com.example.demo.domain.user.entity.UserStatus;

public class UserConverter {

    public static User toUser(UserRequestDto.RegisterUserRequest request) {
        return User.builder()
                .name(request.getName())
                .birthDate(request.getBirthDate())
                .gender(request.getGender())
                .nickname(request.getNickname())
                .role(Role.USER)
                .accountNumber(request.getAccountNumber())
                .status(UserStatus.ACTIVE)
                .provider(request.getProvider())
                .providerUserId(request.getProviderUserId())
                .build();
    }

    public static UserResponseDto.RegisterUserResponse toRegisterResponse(User user) {
        return UserResponseDto.RegisterUserResponse.builder()
                .userId(user.getId())
                .nickname(user.getNickname())
                .role(user.getRole())
                .status(user.getStatus())
                .build();
    }
}
