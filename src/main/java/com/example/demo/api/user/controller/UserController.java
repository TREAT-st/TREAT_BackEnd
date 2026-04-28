package com.example.demo.api.user.controller;

import com.example.demo.api.common.dto.ApiResponseDto;
import com.example.demo.api.user.dto.UserRequestDto.RegisterUserRequest;
import com.example.demo.api.user.dto.UserRequestDto.UpdateUserRequest;
import com.example.demo.api.user.dto.UserResponseDto.RegisterUserResponse;
import com.example.demo.api.user.dto.UserResponseDto.UpdateUserResponse;
import com.example.demo.api.user.mapper.UserConverter;
import com.example.demo.api.user.service.UserUseCase;
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

    @Operation(summary = "사용자 정보 수정", description = "user의 정보를 수정합니다.")
    @PatchMapping("/{userId}")
    // TODO: @AuthUser 적용 후 @PathVariable 제거
    public ApiResponseDto<UpdateUserResponse> updateUser(@PathVariable Long userId,
                                                         @RequestBody UpdateUserRequest request) {
        User user = userUseCase.editUserAccount(userId, request);

        return ApiResponseDto.onSuccess(UserConverter.toUpdateUserResponse(user));
    }

    // 사용자 삭제 테스트용
    /*
    @Operation(summary = "사용자 삭제", description = "사용자 삭제 테스트용입니다.")
    @DeleteMapping
    public ApiResponseDto<Long> deleteUser(@RequestBody Long userId) {
        return ApiResponseDto.onSuccess(userUseCase.deleteUser(userId));
    }
    */
}
