package com.example.demo.api.kis.service;

import com.example.demo.api.kis.dto.KisTokenResponseDto;
import com.example.demo.common.service.RedisService;
import com.example.demo.domain.stock.exception.StockHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class KisTokenService {

    private static final String TOKEN_KEY = "kis:access_token";
    private static final DateTimeFormatter EXPIRED_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final WebClient webClient;
    private final RedisService redisService;

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

        Duration ttl = calculateTtl(tokenResponse.getAccessTokenTokenExpired());
        redisService.setKisTokenExpiresValueWithTtl(TOKEN_KEY, token, ttl);
        log.info("KIS 토큰 Redis 저장 완료. TTL={}s", ttl.getSeconds());

        return token;
    }

    private Duration calculateTtl(String expiredAt) {
        try {
            ZonedDateTime expiry = LocalDateTime.parse(expiredAt, EXPIRED_FORMATTER)
                    .atZone(ZoneId.of("Asia/Seoul"));
            ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Seoul"));
            Duration ttl = Duration.between(now, expiry).minusMinutes(5);

            return (ttl.isNegative() || ttl.isZero()) ? Duration.ofMinutes(1) : ttl;
        } catch (Exception e) {
            log.warn("KIS 토큰 만료 시각 파싱 실패. 기본값 23시간 적용. expiredAt={}", expiredAt);
            return Duration.ofHours(23);
        }
    }
}
