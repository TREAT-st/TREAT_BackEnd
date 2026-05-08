package com.example.demo.security.oauth.handler;

import com.example.demo.common.util.CookieUtil;
import com.example.demo.security.jwt.dto.JwtToken;
import com.example.demo.security.jwt.service.TokenService;
import com.example.demo.security.oauth.dto.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final TokenService tokenService;
    private final CookieUtil cookieUtil;

    @Value("${oauth2.redirect-uri}")
    private String redirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        JwtToken jwtToken = tokenService.login(userDetails.getKakaoEmail());

        // RefreshToken을 HttpOnly Cookie에 세팅
        cookieUtil.setTokenResponse(response, jwtToken);

        // AccessToken을 쿼리 파라미터로 담아 프론트엔드(또는 로컬 테스트 페이지)로 리다이렉트
        String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("accessToken", jwtToken.getAccessToken())
                .build().toUriString();

        log.info("OAuth2 로그인 성공 - member: {}", userDetails.getKakaoEmail());
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
