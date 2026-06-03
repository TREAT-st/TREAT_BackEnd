package com.example.demo.common.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;

import static com.example.demo.common.consts.StaticVariable.EXPIRED_FORMATTER;
import static com.example.demo.common.consts.StaticVariable.SEOUL_ZONE;

@Slf4j
@Component
public class RedisUtil {

    /** 토큰의 TTL을 계산해서 반환한다. 반환 형식: "yyyy-MM-dd HH:mm:ss" **/
    public Duration calculateTtl(String expiredAt) {
        if (expiredAt == null || expiredAt.isBlank()) {
            return Duration.ofHours(23);
        }

        try {
            ZonedDateTime expiry = LocalDateTime.parse(expiredAt, EXPIRED_FORMATTER)
                    .atZone(SEOUL_ZONE);

            Duration ttl = Duration.between(
                    ZonedDateTime.now(SEOUL_ZONE),
                    expiry
            ).minusMinutes(5);

            return ttl.compareTo(Duration.ZERO) > 0 ? ttl : Duration.ZERO;

        } catch (DateTimeParseException e) {
            log.warn("토큰 만료 시각 파싱 실패. expiredAt={}", expiredAt, e);
            return Duration.ofHours(23);
        }
    }
}
