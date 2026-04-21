package com.example.demo.api.userPortfolio.controller;

import com.example.demo.api.common.dto.ApiResponseDto;
import com.example.demo.api.userPortfolio.dto.UserPortfolioRequestDto.UpdateUserPortfolioRequest;
import com.example.demo.api.userPortfolio.dto.UserPortfolioResponseDto;
import com.example.demo.api.userPortfolio.mapper.UserPortfolioConverter;
import com.example.demo.domain.userPortfolio.entity.UserPortfolio;
import com.example.demo.domain.userPortfolio.service.UserPortfolioCommandService;
import com.example.demo.domain.userPortfolio.service.UserPortfolioQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "[사용자 포트폴리오 페이지] 사용자 포트폴리오 API")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserPortfolioController {

    private final UserPortfolioCommandService userPortfolioCommandService;
    private final UserPortfolioQueryService userPortfolioQueryService;

    @Operation(summary = "포트폴리오 수정", description = "사용자의 포트폴리오 통계를 수정합니다.")
    @PatchMapping("/{userId}/portfolio")
    public ApiResponseDto<UserPortfolioResponseDto.UserPortfolioResponse> updatePortfolio(
            @PathVariable Long userId,
            @RequestBody @Valid UpdateUserPortfolioRequest request) {
        userPortfolioCommandService.updateUserPortfolio(userId, request);
        UserPortfolio portfolio = userPortfolioQueryService.getUserPortfolioByUserId(userId);
        return ApiResponseDto.onSuccess(UserPortfolioConverter.toUserPortfolioResponse(portfolio));
    }
}
