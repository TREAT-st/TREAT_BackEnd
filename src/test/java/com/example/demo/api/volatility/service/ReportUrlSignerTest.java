package com.example.demo.api.volatility.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Utilities;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 저장된 객체 URL에서 key를 꺼내 서명하는지 검증.
 *
 * 서명은 로컬 계산이라 진짜 S3Presigner를 쓴다. 저장 URL도 S3Utilities로 만든다.
 * 인코딩을 흉내 내면 한글 종목명이 든 실제 key와 다른 문자열을 검증하게 된다.
 */
class ReportUrlSignerTest {

    private static final String BUCKET = "treat-finance-reports";
    private static final String KEY = "000660_SK하이닉스_20260909_analysis.html";
    private static final Region REGION = Region.AP_NORTHEAST_2;

    private final S3Presigner presigner = S3Presigner.builder()
            .region(REGION)
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("AKIATEST", "secret")))
            .build();

    private final ReportUrlSigner signer = new ReportUrlSigner(presigner, BUCKET, Duration.ofMinutes(30));

    @AfterEach
    void tearDown() {
        presigner.close();
    }

    @Test
    void 저장된_객체_URL을_서명된_URL로_바꾼다() {
        String stored = objectUrl(KEY);

        String signed = signer.sign(stored);

        URI uri = URI.create(signed);
        assertThat(uri.getHost()).isEqualTo(URI.create(stored).getHost());
        assertThat(uri.getPath()).isEqualTo("/" + KEY);
        assertThat(uri.getRawQuery())
                .contains("X-Amz-Signature=")
                .contains("X-Amz-Expires=1800")
                .contains("response-content-type=text%2Fhtml");
    }

    @Test
    void 경로_방식_URL에서도_key를_꺼낸다() {
        String pathStyle = "https://s3.ap-northeast-2.amazonaws.com/" + BUCKET + "/005930_report.html";

        assertThat(signer.extractKey(pathStyle)).contains("005930_report.html");
    }

    @Test
    void 가상_호스트_방식_URL의_인코딩된_key를_풀어서_꺼낸다() {
        assertThat(signer.extractKey(objectUrl(KEY))).contains(KEY);
    }

    @Test
    void 리포트가_아직_없으면_null을_그대로_돌려준다() {
        assertThat(signer.sign(null)).isNull();
    }

    /** 다른 버킷이나 외부 주소를 우리 버킷 key로 착각해 서명하면 엉뚱한 객체를 가리킨다. */
    @Test
    void 리포트_버킷의_URL이_아니면_서명하지_않는다() {
        String other = "https://other-bucket.s3.ap-northeast-2.amazonaws.com/" + KEY;
        String external = "https://example.com/report.html";

        assertThat(signer.sign(other)).isEqualTo(other);
        assertThat(signer.sign(external)).isEqualTo(external);
    }

    private String objectUrl(String key) {
        return S3Utilities.builder().region(REGION).build()
                .getUrl(GetUrlRequest.builder().bucket(BUCKET).key(key).build())
                .toExternalForm();
    }
}
