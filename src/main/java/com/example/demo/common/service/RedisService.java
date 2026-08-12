package com.example.demo.common.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisService {
    private final RedisTemplate redisTemplate;

    //FOR Refresh token(whiteList)
    // key-value 설정
    public void setValue(String token, String username) {
        ValueOperations<String, String> values = redisTemplate.opsForValue();
        values.set(token, username, Duration.ofDays(7));
    }

    // key 값으로 value 가져오기
    public String getValue(String token) {
        ValueOperations<String, String> values = redisTemplate.opsForValue();
        return values.get(token);
    }

    public void deleteValue(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            // "Bearer " 접두사 제거
            token = token.substring(7);
        }
        redisTemplate.delete(token);
    }

    // FOR OAuth2 일회용 auth code (whiteList)
    private static final String AUTH_CODE_PREFIX = "auth:code:";

    public void setAuthCode(String code, String username) {
        ValueOperations<String, String> values = redisTemplate.opsForValue();
        values.set(AUTH_CODE_PREFIX + code, username, Duration.ofMinutes(3));
    }

    public String getAuthCode(String code) {
        ValueOperations<String, String> values = redisTemplate.opsForValue();
        return values.get(AUTH_CODE_PREFIX + code);
    }

    public void deleteAuthCode(String code) {
        redisTemplate.delete(AUTH_CODE_PREFIX + code);
    }

}
