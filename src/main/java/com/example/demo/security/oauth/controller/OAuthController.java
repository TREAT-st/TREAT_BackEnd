package com.example.demo.security.oauth.controller;

import com.example.demo.api.common.dto.ApiResponseDto;
import com.example.demo.common.annotation.DisableSwaggerSecurity;
import com.example.demo.common.consts.StaticVariable;
import com.example.demo.security.jwt.dto.AccessTokenResponse;
import com.example.demo.security.jwt.dto.AuthCodeRequest;
import com.example.demo.security.jwt.dto.JwtToken;
import com.example.demo.security.jwt.service.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@Slf4j
@Tag(name = "OAuth2 API", description = "소셜 로그인 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class OAuthController {

    private final TokenService tokenService;

    /**
     * Swagger에서 클릭하면 카카오 OAuth2 로그인 페이지로 리다이렉트됩니다.
     * 로그인 성공 후 treat://oauth/kakao?code=<uuid> 로 리다이렉트됩니다.
     */
    @Operation(
            summary = "카카오 로그인 시작",
            description = """
                    **[Swagger 카카오 로그인 테스트 방법]**
                    1. 'Try it out' → 'Execute' 클릭
                    2. 브라우저가 카카오 로그인 페이지로 이동합니다 (302 리다이렉트)
                    3. 카카오 계정으로 로그인합니다
                    4. 로그인 성공 시 `treat://oauth/kakao?code=<UUID>` 로 리다이렉트됩니다
                    5. 받은 `code` 값으로 `/api/auth/token` 을 호출해 accessToken/refreshToken을 발급받으세요
                    """
    )
    @DisableSwaggerSecurity
    @GetMapping("/kakao")
    public void kakaoLogin(HttpServletResponse response) throws IOException {
        response.sendRedirect(StaticVariable.KAKAO_OAUTH2_AUTHORIZATION_URI);
    }

    @Operation(
            summary = "일회용 code로 토큰 발급",
            description = """
                    카카오 로그인 성공 후 딥링크로 전달된 일회용 `code`를 교환해 AccessToken/RefreshToken을 발급합니다.
                    - code는 **3분** 유효, **1회만** 사용 가능합니다.
                    - 만료 또는 재사용 시 4360 에러가 반환됩니다.
                    """
    )
    @DisableSwaggerSecurity
    @PostMapping("/token")
    public ApiResponseDto<AccessTokenResponse> exchangeToken(@RequestBody AuthCodeRequest request,
                                                             HttpServletResponse response) {
        JwtToken jwtToken = tokenService.issueTokensByAuthCode(request.getCode());
        response.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken.getAccessToken());
        return ApiResponseDto.onSuccess(new AccessTokenResponse(jwtToken.getAccessToken(), jwtToken.getRefreshToken()));
    }
}