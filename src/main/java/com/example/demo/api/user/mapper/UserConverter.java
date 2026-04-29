package com.example.demo.api.user.mapper;

import com.example.demo.api.user.dto.UserRequestDto.*;
import com.example.demo.api.user.dto.UserResponseDto.*;
import com.example.demo.domain.user.entity.Role;
import com.example.demo.domain.user.entity.User;
import com.example.demo.domain.user.entity.UserStatus;
import com.example.demo.api.user.dto.UserResponseDto.UpdateUserResponse;

public class UserConverter {

    public static User toUser(RegisterUserRequest request) {
        return User.builder()
                .username(request.getProvider().name() + "_" + request.getProviderUserId())
                .kakaoEmail(request.getKakaoEmail())
                .profileImg(request.getProfileImg())
                .name(request.getName())
                .birthDate(request.getBirthDate())
                .gender(request.getGender())
                .nickname(request.getNickname())
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .accountNumber(request.getAccountNumber())
                .provider(request.getProvider())
                .providerUserId(request.getProviderUserId())
                .build();
    }

    public static RegisterUserResponse toRegisterResponse(User user) {
        return RegisterUserResponse.builder()
                .userId(user.getId())
                .nickname(user.getNickname())
                .role(user.getRole())
                .status(user.getStatus())
                .build();
    }

    public static UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .userId(user.getId())
                .kakaoEmail(user.getKakaoEmail())
                .profileImg(user.getProfileImg())
                .name(user.getName())
                .birthdate(user.getBirthDate())
                .gender(user.getGender())
                .nickname(user.getNickname())
                .role(user.getRole())
                .accountNumber(user.getAccountNumber())
                .status(user.getStatus())
                .provider(user.getProvider())
                .build();
    }

    public static UpdateUserResponse toUpdateUserResponse(User user) {
        return UpdateUserResponse.builder()
                .userId(user.getId())
                .name(user.getName())
                .nickname(user.getNickname())
                .profileImg(user.getProfileImg())
                .birthDate(user.getBirthDate())
                .gender(user.getGender())
                .accountNumber(user.getAccountNumber())
                .build();
    }
}
