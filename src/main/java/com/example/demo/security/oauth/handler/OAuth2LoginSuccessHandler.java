package com.example.demo.security.oauth.handler;

import com.example.demo.common.service.RedisService;
import com.example.demo.domain.user.entity.User;
import com.example.demo.domain.user.service.UserQueryService;
import com.example.demo.security.oauth.dto.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final RedisService redisService;
    private final UserQueryService userQueryService;
    private final String redirectUri;

    public OAuth2LoginSuccessHandler(RedisService redisService,
                                     UserQueryService userQueryService,
                                     @Value("${oauth2.redirect-uri}") String redirectUri) {
        this.redisService = redisService;
        this.userQueryService = userQueryService;
        this.redirectUri = redirectUri;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User user = userQueryService.getUserByKakaoEmail(userDetails.getKakaoEmail());

        // 일회용 auth code 생성 후 Redis에 저장 (TTL 3분)
        String authCode = UUID.randomUUID().toString();
        redisService.setAuthCode(authCode, user.getUsername());

        log.info("OAuth2 로그인 성공 - member: {}, redirect: {}?code={}", userDetails.getKakaoEmail(), redirectUri, authCode);

        response.sendRedirect(redirectUri + "?code=" + authCode);
    }
}
