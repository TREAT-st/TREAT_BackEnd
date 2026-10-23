package com.example.demo.security.batch;

import com.example.demo.domain.batch.exception.BatchHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * 내부 자동화 API(/api/v1/batch/**)의 인증. 사용자 토큰이 없는 호출이라 JWT 대신 X-Batch-Secret 헤더로 검증한다.
 * SecurityConfig는 이 경로를 permitAll로 열어두므로, 이 경로의 컨트롤러는 모든 엔드포인트에서 이 검증을 불러야 한다.
 *
 * 리포트 콜백 시크릿과 다른 값이다.
 * 콜백 시크릿은 S3 이벤트 Lambda가 쓰고 이건 배치 트리거가 쓴다. 하나가 새도 다른 하나는 살아 있어야 한다.
 *
 * 오류 코드는 기존 호출자와의 호환을 위해 배치 도메인의 BATCH_TRIGGER_UNAUTHORIZED를 그대로 쓴다.
 */
@Component
public class BatchSecretVerifier {

    private final byte[] triggerSecret;

    public BatchSecretVerifier(@Value("${batch.trigger.secret}") String triggerSecret) {
        this.triggerSecret = triggerSecret.getBytes(StandardCharsets.UTF_8);
    }

    /** 길이가 달라도 시간이 새지 않도록 상수시간 비교를 쓴다. */
    public void verify(String secret) {
        if (secret == null || !MessageDigest.isEqual(secret.getBytes(StandardCharsets.UTF_8), triggerSecret)) {
            throw BatchHandler.triggerUnauthorized();
        }
    }
}
