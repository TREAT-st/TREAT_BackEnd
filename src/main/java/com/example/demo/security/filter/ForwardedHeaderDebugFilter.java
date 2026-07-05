package com.example.demo.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ForwardedHeaderDebugFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String uri = request.getRequestURI();

        if (!uri.startsWith("/actuator") && !uri.startsWith("/v3/api-docs") && !uri.startsWith("/swagger-ui")) {
            log.info("forwarded header debug - method: {}, uri: {}, query: {}, scheme: {}, serverName: {}, serverPort: {}, host: {}, forwarded: {}, X-Forwarded-Proto: {}, X-Forwarded-Host: {}, X-Forwarded-Port: {}, X-Forwarded-For: {}",
                    request.getMethod(),
                    uri,
                    request.getQueryString(),
                    request.getScheme(),
                    request.getServerName(),
                    request.getServerPort(),
                    request.getHeader("Host"),
                    request.getHeader("Forwarded"),
                    request.getHeader("X-Forwarded-Proto"),
                    request.getHeader("X-Forwarded-Host"),
                    request.getHeader("X-Forwarded-Port"),
                    request.getHeader("X-Forwarded-For"));
        }

        filterChain.doFilter(request, response);
    }
}
