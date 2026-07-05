package com.example.demo.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
public class ForwardedHeaderDebugFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String uri = request.getRequestURI();

        if (uri.startsWith("/oauth2/") || uri.startsWith("/login/oauth2/") || uri.startsWith("/api/auth/")) {
            log.info("request forwarded header debug - uri: {}, scheme: {}, serverName: {}, serverPort: {}, X-Forwarded-Proto: {}, X-Forwarded-Port: {}",
                    uri,
                    request.getScheme(),
                    request.getServerName(),
                    request.getServerPort(),
                    request.getHeader("X-Forwarded-Proto"),
                    request.getHeader("X-Forwarded-Port"));
        }

        filterChain.doFilter(request, response);
    }
}
