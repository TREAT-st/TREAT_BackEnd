package com.example.demo.security.controller;

import com.example.demo.api.common.dto.ApiResponseDto;
import com.example.demo.common.annotation.DisableSwaggerSecurity;
import com.example.demo.common.exception.ErrorStatus;
import com.example.demo.common.exception.GeneralException;
import com.example.demo.common.util.CookieUtil;
import com.example.demo.security.filter.JwtAuthenticationFilter;
import com.example.demo.security.jwt.dto.AccessTokenResponse;
import com.example.demo.security.jwt.dto.JwtToken;
import com.example.demo.security.jwt.service.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import static com.example.demo.common.consts.StaticVariable.REFRESH_TOKEN_COOKIE;

@Tag(name = "Token API", description = "토큰 API")
@ApiResponse(responseCode = "2000", description = "성공")
@RequiredArgsConstructor
@RequestMapping("/api/tokens")
@RestController
public class TokenApiController {

    private final CookieUtil cookieUtil;
    private final TokenService tokenService;

    @Operation(
            summary = "로그인(토큰 생성)",
            description = "카카오 이메일로 로그인합니다. AccessToken은 헤더(Authorization)와 바디로, RefreshToken은 HttpOnly Cookie로 전달됩니다."
    )
    @DisableSwaggerSecurity
    @GetMapping("/login")
    public ApiResponseDto<AccessTokenResponse>  login(@RequestParam String kakaoEmail,
                                                     HttpServletResponse response) {
        JwtToken jwtToken = tokenService.login(kakaoEmail);
        cookieUtil.setTokenResponse(response, jwtToken);
        return ApiResponseDto.onSuccess(new AccessTokenResponse(jwtToken.getAccessToken()));
    }

    @Operation(
            summary = "토큰 재발행",
            description = "Cookie의 RefreshToken으로 새 AccessToken을 발급합니다. AccessToken은 헤더(Authorization)와 바디로, 새 RefreshToken은 Cookie로 전달됩니다."
    )
    @DisableSwaggerSecurity
    @PostMapping("/reissue")
    public ApiResponseDto<AccessTokenResponse> reissue(HttpServletRequest request,
                                                       HttpServletResponse response) {
        String refreshToken = cookieUtil.extractRefreshTokenFromCookie(request);
        JwtToken jwtToken = tokenService.issueTokens(refreshToken);
        cookieUtil.setTokenResponse(response, jwtToken);
        return ApiResponseDto.onSuccess(new AccessTokenResponse(jwtToken.getAccessToken()));
    }

    @Operation(
            summary = "로그아웃",
            description = "Cookie의 RefreshToken을 삭제하고 Redis에서 제거합니다."
    )
    @PostMapping("/logout")
    public ApiResponseDto<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = cookieUtil.extractRefreshTokenFromCookie(request);
        tokenService.logout(refreshToken);
        cookieUtil.expireRefreshTokenCookie(response);
        return ApiResponseDto.onSuccess(null);
    }


}
