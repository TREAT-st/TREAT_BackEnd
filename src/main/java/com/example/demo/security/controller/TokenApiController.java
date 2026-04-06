package com.example.demo.security.controller;

import com.example.demo.api.common.dto.ApiResponseDto;
import com.example.demo.common.annotation.DisableSwaggerSecurity;
import com.example.demo.common.exception.ErrorStatus;
import com.example.demo.common.exception.GeneralException;
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

@Tag(name = "Token API", description = "토큰 API")
@ApiResponse(responseCode = "2000", description = "성공")
@RequiredArgsConstructor
@RequestMapping("/api/tokens")
@RestController
public class TokenApiController {

    private static final String REFRESH_TOKEN_COOKIE = "refreshToken";

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
        setTokenResponse(response, jwtToken);
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
        String refreshToken = extractRefreshTokenFromCookie(request);
        JwtToken jwtToken = tokenService.issueTokens(refreshToken);
        setTokenResponse(response, jwtToken);
        return ApiResponseDto.onSuccess(new AccessTokenResponse(jwtToken.getAccessToken()));
    }

    @Operation(
            summary = "로그아웃",
            description = "Cookie의 RefreshToken을 삭제하고 Redis에서 제거합니다."
    )
    @PostMapping("/logout")
    public ApiResponseDto<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = extractRefreshTokenFromCookie(request);
        tokenService.logout(refreshToken);
        expireRefreshTokenCookie(response);
        return ApiResponseDto.onSuccess(null);
    }

    /** AccessToken을 헤더에, RefreshToken을 HttpOnly Cookie에 세팅한다. */
    private void setTokenResponse(HttpServletResponse response, JwtToken jwtToken) {
        response.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken.getAccessToken());
        response.addCookie(createRefreshTokenCookie(jwtToken.getRefreshToken()));
    }

    private Cookie createRefreshTokenCookie(String refreshToken) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE, refreshToken);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(7 * 24 * 60 * 60); // 7일
        return cookie;
    }

    /** 만료된 빈 Cookie를 덮어써서 브라우저의 RefreshToken Cookie를 삭제한다. */
    private void expireRefreshTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE, null);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    private String extractRefreshTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (REFRESH_TOKEN_COOKIE.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        throw new GeneralException(ErrorStatus.AUTH_REFRESH_TOKEN_NOT_FOUND);
    }
}
