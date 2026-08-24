package com.example.demo.security.controller;

import com.example.demo.api.common.dto.ApiResponseDto;
import com.example.demo.common.annotation.DisableSwaggerSecurity;
import com.example.demo.security.jwt.dto.AccessTokenResponse;
import com.example.demo.security.jwt.dto.JwtToken;
import com.example.demo.security.jwt.dto.RefreshTokenRequest;
import com.example.demo.security.jwt.service.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Token API", description = "토큰 API")
@ApiResponse(responseCode = "2000", description = "성공")
@RequiredArgsConstructor
@RequestMapping("/api/tokens")
@RestController
public class TokenApiController {

    private final TokenService tokenService;

    @Operation(
            summary = "로그인(토큰 생성)",
            description = "카카오 이메일로 로그인합니다. AccessToken, RefreshToken 둘 다 body로 전달됩니다."
    )
    @DisableSwaggerSecurity
    @GetMapping("/login")
    public ApiResponseDto<AccessTokenResponse> login(@RequestParam String kakaoEmail,
                                                     HttpServletResponse response) {
        JwtToken jwtToken = tokenService.login(kakaoEmail);
        response.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken.getAccessToken());
        return ApiResponseDto.onSuccess(new AccessTokenResponse(jwtToken.getAccessToken(), jwtToken.getRefreshToken()));
    }

    @Operation(
            summary = "토큰 재발행",
            description = "body의 RefreshToken으로 새 AccessToken을 발급합니다. AccessToken은 헤더(Authorization)와 body로 전달됩니다."
    )
    @DisableSwaggerSecurity
    @PostMapping("/reissue")
    public ApiResponseDto<AccessTokenResponse> reissue(@RequestBody RefreshTokenRequest request,
                                                       HttpServletResponse response) {
        JwtToken jwtToken = tokenService.issueTokens(request.getRefreshToken());
        response.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken.getAccessToken());
        return ApiResponseDto.onSuccess(new AccessTokenResponse(jwtToken.getAccessToken(), jwtToken.getRefreshToken()));
    }

    @Operation(
            summary = "로그아웃",
            description = "헤더(Authorization)의 AccessToken과 body의 RefreshToken을 받아 Redis에서 RefreshToken을 제거합니다."
    )
    @PostMapping("/logout")
    public ApiResponseDto<Void> logout(@RequestBody RefreshTokenRequest request) {
        tokenService.logout(request.getRefreshToken());
        return ApiResponseDto.onSuccess(null);
    }
}