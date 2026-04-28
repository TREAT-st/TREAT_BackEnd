package com.example.demo.api.user.controller;

import com.example.demo.api.common.dto.ApiResponseDto;
import com.example.demo.api.user.dto.UserRequestDto.*;
import com.example.demo.api.user.dto.UserResponseDto.*;
import com.example.demo.api.user.mapper.UserConverter;
import com.example.demo.api.user.service.UserUseCase;
import com.example.demo.common.annotation.AuthUser;
import com.example.demo.domain.user.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "[사용자 페이지] 사용자 계정 API")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final UserUseCase userUseCase;

    @Operation(summary = "회원 가입", description = "사용자를 등록하고 포트폴리오를 자동 생성합니다.")
    @PostMapping
    public ApiResponseDto<RegisterUserResponse> registerUser(@RequestBody RegisterUserRequest request) {
        User user = UserConverter.toUser(request);
        User registeredUser = userUseCase.signUpWithUserPortfolio(user);

        return ApiResponseDto.onSuccess(UserConverter.toRegisterResponse(registeredUser));
    }

    @Operation(summary = "사용자 정보 조회", description = "user의 정보를 조회합니다.")
    @GetMapping("/{userId}")
    public ApiResponseDto<UserResponse> getUserByUsername(@PathVariable Long userId) {
        User user = userUseCase.getUserByUserId(userId);

        return ApiResponseDto.onSuccess(UserConverter.toUserResponse(user));
    }

    @Operation(summary = "사용자 정보 수정", description = "user의 정보를 수정합니다.")
    @PatchMapping
    public ApiResponseDto<UpdateUserResponse> updateUser(@AuthUser User user,
                                                         @RequestBody UpdateUserRequest request) {
        User updatedUser = userUseCase.editUserAccount(user.getId(), request);

        return ApiResponseDto.onSuccess(UserConverter.toUpdateUserResponse(updatedUser));
    }
}
