package com.example.demo.api.volatility.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.net.URI;
import java.time.Duration;
import java.util.Optional;

/**
 * 리포트 버킷은 private이라 DB에 저장된 객체 URL로는 열리지 않는다(403).
 * 조회 응답을 만들 때마다 짧은 유효기간의 presigned URL로 바꿔 내려준다.
 *
 * DB에는 콜백과 보정이 넣은 객체 URL을 그대로 둔다. key는 URL에서 꺼낸다.
 * 서명 URL을 저장하면 만료된 링크가 남으므로 저장하지 않는다.
 */
@Slf4j
@Component
public class ReportUrlSigner {

    /** S3가 Content-Type 없이 올라간 파일을 다운로드로 내려도 브라우저가 열 수 있게 한다. */
    private static final String HTML_CONTENT_TYPE = "text/html; charset=utf-8";

    private final S3Presigner s3Presigner;
    private final String bucket;
    private final Duration ttl;

    public ReportUrlSigner(S3Presigner s3Presigner,
                           @Value("${cloud.aws.s3.bucket.finance-reports}") String bucket,
                           @Value("${cloud.aws.s3.presign-ttl.finance-reports:PT30M}") Duration ttl) {
        this.s3Presigner = s3Presigner;
        this.bucket = bucket;
        this.ttl = ttl;
    }

    /**
     * @return 서명된 URL. 리포트가 아직 없으면 null.
     *         우리 버킷의 URL이 아니거나 서명에 실패하면 저장된 값을 그대로 돌려준다.
     *         한 건의 실패로 목록 조회 전체를 깨뜨리지 않기 위해서다.
     */
    public String sign(String storedUrl) {
        if (storedUrl == null || storedUrl.isBlank()) {
            return storedUrl;
        }

        Optional<String> key = extractKey(storedUrl);
        if (key.isEmpty()) {
            log.warn("리포트 버킷의 URL이 아니라 서명하지 않습니다. url={}", storedUrl);
            return storedUrl;
        }

        try {
            return s3Presigner.presignGetObject(GetObjectPresignRequest.builder()
                            .signatureDuration(ttl)
                            .getObjectRequest(GetObjectRequest.builder()
                                    .bucket(bucket)
                                    .key(key.get())
                                    .responseContentType(HTML_CONTENT_TYPE)
                                    .build())
                            .build())
                    .url()
                    .toExternalForm();
        } catch (Exception e) {
            log.error("리포트 URL 서명에 실패했습니다. key={}", key.get(), e);
            return storedUrl;
        }
    }

    /**
     * 가상 호스트 방식(bucket.s3.region.amazonaws.com/key)과
     * 경로 방식(s3.region.amazonaws.com/bucket/key)을 모두 받는다.
     * 콜백 URL은 ECS가 만들어 형식을 보장할 수 없다.
     */
    Optional<String> extractKey(String storedUrl) {
        URI uri;
        try {
            uri = URI.create(storedUrl.trim());
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }

        String host = uri.getHost();
        String path = uri.getPath(); // 퍼센트 인코딩이 풀린 값이다.
        if (host == null || path == null) {
            return Optional.empty();
        }

        String key = null;
        if (host.startsWith(bucket + ".s3")) {
            key = path.startsWith("/") ? path.substring(1) : path;
        } else if (host.startsWith("s3") && path.startsWith("/" + bucket + "/")) {
            key = path.substring(bucket.length() + 2);
        }

        return Optional.ofNullable(key).filter(k -> !k.isBlank());
    }
}
