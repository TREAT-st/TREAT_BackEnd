package com.example.demo.api.kis.service;

import com.example.demo.api.kis.client.KisFeignClient;
import com.example.demo.api.kis.dto.KisTokenRequestDto;
import com.example.demo.api.kis.dto.KisTokenResponseDto;
import com.example.demo.common.service.RedisService;
import com.example.demo.common.util.RedisUtil;
import com.example.demo.api.kis.exception.KisHandler;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;

import static com.example.demo.common.consts.StaticVariable.GRANT_TYPE;
import static com.example.demo.common.consts.StaticVariable.TOKEN_KEY;

@Slf4j
@Service
@RequiredArgsConstructor
public class KisTokenService {

    private final KisFeignClient kisFeignClient;
    private final RedisService redisService;
    private final RedisUtil redisUtil;

    @Value("${kis.app-key}")
    private String appKey;

    @Value("${kis.app-secret}")
    private String appSecret;

    public String getAccessToken() {
        String cached = redisService.getValue(TOKEN_KEY);
        if (cached != null && !cached.isBlank()) {
            log.debug("KIS 토큰 캐시 히트");
            return cached;
        }

        log.info("KIS 토큰 신규 발급 요청");

        try {
            KisTokenResponseDto tokenResponse = kisFeignClient.issueToken(
                    new KisTokenRequestDto(GRANT_TYPE, appKey, appSecret)
            );

            if (tokenResponse == null || tokenResponse.getAccessToken() == null
                    || tokenResponse.getAccessToken().isBlank()) {
                throw KisHandler.kisApiError();
            }

            String token = tokenResponse.getAccessToken();
            Duration ttl = redisUtil.calculateTtl(tokenResponse.getAccessTokenTokenExpired());
            redisService.setKisTokenExpiresValueWithTtl(TOKEN_KEY, token, ttl);
            log.info("KIS 토큰 Redis 저장 완료. TTL={}s", ttl.getSeconds());

            return token;

        } catch (FeignException e) {
            log.error("KIS 토큰 발급 실패. status={}", e.status());
            throw KisHandler.kisApiError();
        }
    }
}