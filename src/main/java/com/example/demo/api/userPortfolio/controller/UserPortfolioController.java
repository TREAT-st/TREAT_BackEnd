package com.example.demo.api.userPortfolio.controller;

import com.example.demo.api.common.dto.ApiResponseDto;
import com.example.demo.api.userPortfolio.dto.UserPortfolioRequestDto.*;
import com.example.demo.api.userPortfolio.dto.UserPortfolioResponseDto.UserPortfolioResponse;
import com.example.demo.api.userPortfolio.mapper.UserPortfolioConverter;
import com.example.demo.api.userPortfolio.service.UserPortfolioUseCase;
import com.example.demo.common.annotation.AuthUser;
import com.example.demo.domain.user.entity.User;
import com.example.demo.domain.userPortfolio.entity.UserPortfolio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "[사용자 포트폴리오 페이지] 사용자 포트폴리오 API")
@RestController
@RequestMapping("/api/v1/users/portfolio")
@RequiredArgsConstructor
public class UserPortfolioController {
    private final UserPortfolioUseCase userPortfolioUseCase;

    @Operation(summary = "포트폴리오 조회", description = "사용자의 포트폴리오를 조회합니다.")
    @GetMapping
    public ApiResponseDto<UserPortfolioResponse> getUserPortfolio(@AuthUser User user) {
        UserPortfolio portfolio = userPortfolioUseCase.getUserPortfolio(user.getId());

        return ApiResponseDto.onSuccess(UserPortfolioConverter.toUserPortfolioResponse(portfolio));
    }

    @Operation(summary = "포트폴리오 삭제", description = "사용자의 포트폴리오를 삭제합니다.")
    @DeleteMapping
    public ApiResponseDto<Long> deleteUserPortfolio(@AuthUser User user) {
        return ApiResponseDto.onSuccess(userPortfolioUseCase.deleteUserPortfolio(user.getId()));
    }
}
