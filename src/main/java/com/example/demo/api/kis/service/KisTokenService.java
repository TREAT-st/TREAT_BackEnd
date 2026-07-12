package com.example.demo.api.kis.service;

import com.example.demo.api.kis.client.KisFeignClient;
import com.example.demo.api.kis.dto.KisTokenRequestDto;
import com.example.demo.api.kis.dto.KisTokenResponseDto;
import com.example.demo.common.service.RedisService;
import com.example.demo.common.util.RedisUtil;
import com.example.demo.domain.stock.exception.StockHandler;
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
        log.info("[KIS 토큰] 1. Redis 캐시 조회 시작. key={}", TOKEN_KEY);
        String cached = redisService.getValue(TOKEN_KEY);

        if (cached != null && !cached.isBlank()) {
            log.info("[KIS 토큰] 2. 캐시 히트 - 저장된 토큰 반환");
            return cached;
        }
        log.info("[KIS 토큰] 2. 캐시 미스 - 신규 발급 시작");

        log.info("[KIS 토큰] 3. KIS API 토큰 발급 요청. appKey={}", appKey);
        try {
            KisTokenResponseDto tokenResponse = kisFeignClient.issueToken(
                    new KisTokenRequestDto(GRANT_TYPE, appKey, appSecret)
            );
            log.info("[KIS 토큰] 4. KIS API 응답 수신. response={}", tokenResponse);

            if (tokenResponse == null || tokenResponse.getAccessToken() == null
                    || tokenResponse.getAccessToken().isBlank()) {
                log.error("[KIS 토큰] 5. 응답 토큰 값 없음 (null 또는 blank)");
                throw StockHandler.kisApiError();
            }
            log.info("[KIS 토큰] 5. 응답 토큰 정상 확인. expiredAt={}", tokenResponse.getAccessTokenTokenExpired());

            String token = tokenResponse.getAccessToken();

            log.info("[KIS 토큰] 6. TTL 계산 시작. expiredAt={}", tokenResponse.getAccessTokenTokenExpired());
            Duration ttl = redisUtil.calculateTtl(tokenResponse.getAccessTokenTokenExpired());
            log.info("[KIS 토큰] 7. TTL 계산 완료. ttl={}s", ttl.getSeconds());

            log.info("[KIS 토큰] 8. Redis 저장 시작. key={}, ttl={}s", TOKEN_KEY, ttl.getSeconds());
            redisService.setKisTokenExpiresValueWithTtl(TOKEN_KEY, token, ttl);
            log.info("[KIS 토큰] 9. Redis 저장 완료");

            return token;

        } catch (FeignException e) {
            log.error("[KIS 토큰] FeignException 발생. status={}, message={}", e.status(), e.getMessage());
            throw StockHandler.kisApiError();
        }
    }
}