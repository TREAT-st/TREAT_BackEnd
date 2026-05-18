package com.example.demo.api.kis.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class KisTokenService {

    private final WebClient webClient;

    @Value("${kis.app-key}")
    private String appKey;

    @Value("${kis.app-secret}")
    private String appSecret;

    public String getAccessToken() {
        Map<String, String> body = Map.of(
                "grant_type", "client_credentials",
                "appkey", appKey,
                "appsecret", appSecret
        );

        return webClient.post()
                .uri("/oauth2/tokenP")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .map(res -> (String) res.get("access_token"))
                .block();
    }
}
