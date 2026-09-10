package com.example.demo.api.batch.service;

import com.example.demo.api.batch.service.ReportUrlRecoverer.RecoveryResult;
import com.example.demo.domain.volatility.entity.Volatility;
import com.example.demo.domain.volatility.service.VolatilityCommandService;
import com.example.demo.domain.volatility.service.VolatilityQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Utilities;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 콜백만 유실된 리포트를 S3에서 찾아 메우는 로직 검증.
 *
 * S3Client는 목이지만 utilities()는 진짜를 쓴다. URL 인코딩을 흉내 내면
 * 실제와 다른 문자열을 검증하게 된다.
 */
class ReportUrlRecovererTest {

    private static final String BUCKET = "treat-finance-reports";
    private static final LocalDate TRADE_DATE = LocalDate.of(2026, 9, 9);
    private static final String KEY = "000660_SK하이닉스_20260909_analysis.html";

    private final S3Client s3Client = Mockito.mock(S3Client.class);
    private final VolatilityQueryService queryService = Mockito.mock(VolatilityQueryService.class);
    private final VolatilityCommandService commandService = Mockito.mock(VolatilityCommandService.class);

    private final ReportUrlRecoverer recoverer =
            new ReportUrlRecoverer(s3Client, queryService, commandService);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(recoverer, "bucket", BUCKET);
        ReflectionTestUtils.setField(recoverer, "keyPrefix", "");
        Mockito.when(s3Client.utilities())
                .thenReturn(S3Utilities.builder().region(Region.AP_NORTHEAST_2).build());
        Mockito.when(queryService.getMissingReport(TRADE_DATE)).thenReturn(List.of(missing("000660")));
        Mockito.when(commandService.fillReportUrlIfAbsent(anyString(), any(), anyString())).thenReturn(1);
    }

    @Test
    void 키가_하나_맞으면_URL을_채운다() {
        listing(object(KEY, "2026-09-09T10:00:00Z"));

        RecoveryResult result = recoverer.recover(TRADE_DATE);

        assertThat(result.recovered()).isEqualTo(1);
        assertThat(result.lookupFailures()).isZero();
        verify(commandService).fillReportUrlIfAbsent(eq("000660"), eq(TRADE_DATE),
                Mockito.contains("treat-finance-reports"));
    }

    /** 같은 종목의 과거 리포트가 계속 쌓인다. 거래일로 좁히지 않으면 엉뚱한 날짜를 붙인다. */
    @Test
    void 날짜가_다른_리포트는_고르지_않는다() {
        listing(
                object("000660_SK하이닉스_20260408_analysis.html", "2026-04-08T10:00:00Z"),
                object("000660_SK하이닉스_20260802_analysis.html", "2026-08-02T10:00:00Z"));

        RecoveryResult result = recoverer.recover(TRADE_DATE);

        assertThat(result.recovered()).isZero();
        verify(commandService, never()).fillReportUrlIfAbsent(anyString(), any(), anyString());
    }

    @Test
    void 맞는_키가_없으면_채우지_않는다() {
        listing();

        RecoveryResult result = recoverer.recover(TRADE_DATE);

        assertThat(result.recovered()).isZero();
        assertThat(result.lookupFailures()).isZero();
        verify(commandService, never()).fillReportUrlIfAbsent(anyString(), any(), anyString());
    }

    /**
     * 페이지네이터의 contents()는 페이지를 이어 붙여 흘려준다.
     * 첫 일치에서 멈추거나 앞부분만 보면 뒤쪽 객체를 놓친다.
     */
    @Test
    void 목록_뒤쪽에_있어도_찾는다() {
        listing(
                object("000660_SK하이닉스_20260408_analysis.html", "2026-04-08T10:00:00Z"),
                object("000660_SK하이닉스_20260802_analysis.html", "2026-08-02T10:00:00Z"),
                object("000660_SK하이닉스_20260804_analysis.html", "2026-08-04T10:00:00Z"),
                object(KEY, "2026-09-09T10:00:00Z"));

        assertThat(recoverer.recover(TRADE_DATE).recovered()).isEqualTo(1);
    }

    /** 종목명이 바뀐 뒤 재생성되면 같은 거래일에 두 개가 남는다. 선택이 실행마다 흔들리면 안 된다. */
    @Test
    void 여러_개면_최신을_고른다() {
        listing(
                object("000660_구이름_20260909_analysis.html", "2026-09-09T10:00:00Z"),
                object(KEY, "2026-09-09T18:00:00Z"));

        recoverer.recover(TRADE_DATE);

        verify(commandService).fillReportUrlIfAbsent(anyString(), any(), Mockito.contains("SK"));
    }

    /** 보정은 조회 시점의 판단이라 갱신할 때는 이미 낡았을 수 있다. */
    @Test
    void 콜백이_먼저_채웠으면_보정으로_치지_않는다() {
        listing(object(KEY, "2026-09-09T10:00:00Z"));
        Mockito.when(commandService.fillReportUrlIfAbsent(anyString(), any(), anyString())).thenReturn(0);

        assertThat(recoverer.recover(TRADE_DATE).recovered()).isZero();
    }

    /** 권한 장애를 단순 미도착으로 기록하면 운영자가 원인을 구분할 수 없다. */
    @Test
    void S3_조회_실패는_미도착과_따로_센다() {
        Mockito.when(queryService.getMissingReport(TRADE_DATE))
                .thenReturn(List.of(missing("000660"), missing("005930")));
        ListObjectsV2Iterable second =
                paginator(object("005930_삼성전자_20260909_analysis.html", "2026-09-09T10:00:00Z"));
        Mockito.when(s3Client.listObjectsV2Paginator(any(ListObjectsV2Request.class)))
                .thenThrow(new RuntimeException("AccessDenied"))
                .thenReturn(second);

        RecoveryResult result = recoverer.recover(TRADE_DATE);

        // 앞 종목이 터져도 뒤 종목은 보정된다.
        assertThat(result.lookupFailures()).isEqualTo(1);
        assertThat(result.recovered()).isEqualTo(1);
    }

    @Test
    void 미도착이_없으면_S3를_부르지_않는다() {
        Mockito.when(queryService.getMissingReport(TRADE_DATE)).thenReturn(List.of());

        RecoveryResult result = recoverer.recover(TRADE_DATE);

        assertThat(result.recovered()).isZero();
        verify(s3Client, never()).listObjectsV2Paginator(any(ListObjectsV2Request.class));
    }

    /**
     * 페이지네이터 목을 먼저 완성한 뒤 s3Client를 스터빙한다.
     * thenReturn의 인자 안에서 다시 when을 부르면 중첩 스터빙으로 터진다.
     */
    private void listing(S3Object... objects) {
        ListObjectsV2Iterable iterable = paginator(objects);
        Mockito.when(s3Client.listObjectsV2Paginator(any(ListObjectsV2Request.class)))
                .thenReturn(iterable);
    }

    private ListObjectsV2Iterable paginator(S3Object... objects) {
        ListObjectsV2Iterable iterable = Mockito.mock(ListObjectsV2Iterable.class);
        SdkIterable<S3Object> contents = () -> List.of(objects).iterator();
        Mockito.when(iterable.contents()).thenReturn(contents);
        return iterable;
    }

    private S3Object object(String key, String lastModified) {
        return S3Object.builder().key(key).lastModified(Instant.parse(lastModified)).build();
    }

    private Volatility missing(String stockCode) {
        return Volatility.builder()
                .stockCode(stockCode)
                .stockName("종목")
                .tradeDate(TRADE_DATE)
                .build();
    }
}
