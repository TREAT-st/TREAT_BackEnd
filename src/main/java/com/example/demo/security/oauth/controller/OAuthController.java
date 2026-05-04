package com.example.demo.security.oauth.controller;

import com.example.demo.api.common.dto.ApiResponseDto;
import com.example.demo.common.annotation.DisableSwaggerSecurity;
import com.example.demo.common.consts.StaticVariable;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Tag(name = "OAuth2 API", description = "소셜 로그인 API")
@RestController
@RequestMapping("/api/auth")
public class OAuthController {

    /**
     * Swagger에서 클릭하면 카카오 OAuth2 로그인 페이지로 리다이렉트됩니다.
     * 로그인 성공 후 oauth2.redirect-uri?accessToken=<token> 으로 리다이렉트됩니다.
     */
    @Operation(
            summary = "카카오 로그인 시작",
            description = """
                    **[Swagger 카카오 로그인 테스트 방법]**
                    1. 'Try it out' → 'Execute' 클릭
                    2. 브라우저가 카카오 로그인 페이지로 이동합니다 (302 리다이렉트)
                    3. 카카오 계정으로 로그인합니다
                    4. 로그인 성공 시 `oauth2.redirect-uri?accessToken=<JWT>` 로 리다이렉트됩니다
                    5. URL의 `accessToken` 값을 복사합니다
                    6. Swagger 우상단 **Authorize** 버튼 클릭 → 복사한 토큰 붙여넣기 후 인증
                    """
    )
    @DisableSwaggerSecurity
    @GetMapping("/kakao")
    public void kakaoLogin(HttpServletResponse response) throws IOException {
        response.sendRedirect(StaticVariable.KAKAO_OAUTH2_AUTHORIZATION_URI);
    }

    /**
     * 로컬 테스트 전용: OAuth2 성공 후 리다이렉트되는 콜백 페이지
     * application.yml의 oauth2.redirect-uri가 http://localhost:8080/oauth2/redirect 일 때 사용
     */
    @Operation(
            summary = "OAuth2 콜백 (로컬 테스트용)",
            description = """
                    카카오 로그인 성공 후 리다이렉트되는 엔드포인트입니다.
                    `accessToken` 파라미터에 JWT 액세스 토큰이 포함되어 있습니다.
                    이 토큰을 복사해 Swagger의 Authorize에 입력하세요.
                    """
    )
    @DisableSwaggerSecurity
    @GetMapping("/kakao/callback")
    public ApiResponseDto<Map<String, String>> kakaoCallback(
            @Parameter(description = "OAuth2 성공 핸들러가 발급한 JWT 액세스 토큰")
            @RequestParam(required = false) String accessToken) {
        if (accessToken == null) {
            return ApiResponseDto.onFailure(4001, "accessToken이 없습니다.", null);
        }
        log.info("카카오 로그인 콜백 - accessToken 발급 완료");
        return ApiResponseDto.onSuccess(Map.of("accessToken", accessToken));
    }
}
