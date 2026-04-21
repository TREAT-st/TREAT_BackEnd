package com.example.demo.api.user.controller;

import com.example.demo.api.common.dto.ApiResponseDto;
import com.example.demo.api.user.dto.UserRequestDto.RegisterUserRequest;
import com.example.demo.api.user.dto.UserResponseDto.RegisterUserResponse;
import com.example.demo.api.user.mapper.UserConverter;
import com.example.demo.api.user.service.UserUseCase;
import com.example.demo.domain.user.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
    public ApiResponseDto<RegisterUserResponse> registerUser(
            @RequestBody @Valid RegisterUserRequest request) {
        User user = UserConverter.toUser(request);
        User savedUser = userUseCase.execute(user);
        return ApiResponseDto.onSuccess(UserConverter.toRegisterResponse(savedUser));
    }
}
