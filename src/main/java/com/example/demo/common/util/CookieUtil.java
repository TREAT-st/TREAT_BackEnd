package com.example.demo.common.util;

import com.example.demo.common.exception.ErrorStatus;
import com.example.demo.common.exception.GeneralException;
import com.example.demo.security.jwt.dto.JwtToken;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import static com.example.demo.common.consts.StaticVariable.REFRESH_TOKEN_COOKIE;

// CookieUtil.java
@Component
public class CookieUtil {

    /** AccessToken을 헤더에, RefreshToken을 HttpOnly Cookie에 세팅한다. */
    public void setTokenResponse(HttpServletResponse response, JwtToken jwtToken) {
        response.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken.getAccessToken());
        response.addCookie(createRefreshTokenCookie(jwtToken.getRefreshToken()));
    }

    public Cookie createRefreshTokenCookie(String refreshToken) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE, refreshToken);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(7 * 24 * 60 * 60); // 7일
        return cookie;
    }

    /** 만료된 빈 Cookie를 덮어써서 브라우저의 RefreshToken Cookie를 삭제한다. */
    public void expireRefreshTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE, null);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    public String extractRefreshTokenFromCookie(HttpServletRequest request) {
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
