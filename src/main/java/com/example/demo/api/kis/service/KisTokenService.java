package com.example.demo.api.kis.service;

import com.example.demo.api.kis.dto.KisTokenResponseDto;
import com.example.demo.common.service.RedisService;
import com.example.demo.common.util.RedisUtil;
import com.example.demo.domain.stock.exception.StockHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

import static com.example.demo.common.consts.StaticVariable.TOKEN_KEY;

@Slf4j
@Service
@RequiredArgsConstructor
public class KisTokenService {
    private final WebClient webClient;
    private final RedisService redisService;
    private final RedisUtil redisUtil;

    @Value("${kis.app-key}")
    private String appKey;

    @Value("${kis.app-secret}")
    private String appSecret;

    public String getAccessToken() {
        String cached = redisService.getValue(TOKEN_KEY);
        if (cached != null) {
            log.debug("KIS 토큰 캐시 히트");
            return cached;
        }

        log.info("KIS 토큰 신규 발급 요청");
        Map<String, String> body = Map.of(
                "grant_type", "client_credentials",
                "appkey", appKey,
                "appsecret", appSecret
        );

        KisTokenResponseDto tokenResponse = webClient.post()
                .uri("/oauth2/tokenP")
                .bodyValue(body)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError,
                        res -> Mono.error(StockHandler.kisApiError()))
                .onStatus(HttpStatusCode::is5xxServerError,
                        res -> Mono.error(StockHandler.kisApiError()))
                .bodyToMono(KisTokenResponseDto.class)
                .timeout(Duration.ofSeconds(5))
                .block();

        String token = Objects.requireNonNull(tokenResponse).getAccessToken();

        Duration ttl = redisUtil.calculateTtl(tokenResponse.getAccessTokenTokenExpired());
        redisService.setKisTokenExpiresValueWithTtl(TOKEN_KEY, token, ttl);
        log.info("KIS 토큰 Redis 저장 완료. TTL={}s", ttl.getSeconds());

        return token;
    }
}
