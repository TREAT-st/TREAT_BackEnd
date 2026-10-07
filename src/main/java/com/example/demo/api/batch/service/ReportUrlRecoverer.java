package com.example.demo.api.batch.service;

import com.example.demo.domain.volatility.entity.Volatility;
import com.example.demo.domain.volatility.service.VolatilityCommandService;
import com.example.demo.domain.volatility.service.VolatilityQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * 리포트는 만들어졌는데 콜백만 유실된 경우를 S3에서 찾아 메운다.
 *
 * 도착 확인 스위퍼가 미도착을 발견했을 때만 불린다. 정상 흐름에서는 S3를 한 번도 부르지 않는다.
 *
 * 재요청보다 먼저 와야 한다. 파일이 이미 있는데 다시 요청하면 GPT 비용이 헛나간다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class ReportUrlRecoverer {

    private static final String KEY_SUFFIX = "_analysis.html";

    /** STRICT 해석이라 존재하지 않는 날짜를 보정 없이 거부한다. STRICT에서는 yyyy가 아니라 uuuu다. */
    private static final DateTimeFormatter REPORT_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("uuuuMMdd").withResolverStyle(ResolverStyle.STRICT);

    private final S3Client s3Client;
    private final VolatilityQueryService volatilityQueryService;
    private final VolatilityCommandService volatilityCommandService;

    @Value("${cloud.aws.s3.bucket.finance-reports}")
    private String bucket;

    /** 키가 버킷 루트에 있으면 빈 값. 나중에 디렉터리가 생기면 여기만 바꾸면 된다. */
    @Value("${cloud.aws.s3.key-prefix.finance-reports:}")
    private String keyPrefix;

    /**
     * @return 보정한 건수와 S3 조회에 실패한 건수.
     *         둘을 나눠 돌려주는 이유는, 권한 장애를 단순 미도착으로 기록하면
     *         운영자가 원인을 구분할 수 없기 때문이다.
     */
    public RecoveryResult recover(LocalDate tradeDate) {
        List<Volatility> missing = volatilityQueryService.getMissingReport(tradeDate);
        if (missing.isEmpty()) {
            return new RecoveryResult(0, 0);
        }

        String suffix = "_" + tradeDate.format(REPORT_DATE_FORMATTER) + KEY_SUFFIX;
        int recovered = 0;
        int lookupFailures = 0;

        for (Volatility volatility : missing) {
            // 한 종목의 S3 오류가 나머지 보정을 막지 않는다.
            try {
                if (recoverOne(volatility.getStockCode(), tradeDate, suffix)) {
                    recovered++;
                }
            } catch (Exception e) {
                lookupFailures++;
                log.error("S3 조회에 실패했습니다. stockCode={} tradeDate={}",
                        volatility.getStockCode(), tradeDate, e);
            }
        }

        return new RecoveryResult(recovered, lookupFailures);
    }

    private boolean recoverOne(String stockCode, LocalDate tradeDate, String suffix) {
        Optional<S3Object> found = findLatestMatching(keyPrefix + stockCode + "_", suffix);
        if (found.isEmpty()) {
            log.info("리포트가 아직 S3에 없습니다. stockCode={} tradeDate={}", stockCode, tradeDate);
            return false;
        }

        String key = found.get().key();
        String url = s3Client.utilities()
                .getUrl(GetUrlRequest.builder().bucket(bucket).key(key).build())
                .toExternalForm();

        // 조회 시점과 갱신 시점 사이에 콜백이 채웠을 수 있다. 그러면 콜백의 값이 더 최신이다.
        int updated = volatilityCommandService.fillReportUrlIfAbsent(stockCode, tradeDate, url);
        if (updated == 0) {
            log.info("콜백이 먼저 채워 보정하지 않았습니다. stockCode={} tradeDate={}", stockCode, tradeDate);
            return false;
        }

        log.info("S3에서 리포트를 찾아 보정했습니다. stockCode={} key={}", stockCode, key);
        return true;
    }

    /**
     * 종목코드로만 걸러내면 그 종목의 과거 리포트가 전부 딸려온다.
     * 거래일 suffix로 한 번 더 좁히지 않으면 엉뚱한 날짜의 리포트를 붙이게 된다.
     */
    private Optional<S3Object> findLatestMatching(String prefix, String suffix) {
        List<S3Object> matches = new ArrayList<>();

        s3Client.listObjectsV2Paginator(ListObjectsV2Request.builder()
                        .bucket(bucket)
                        .prefix(prefix)
                        .build())
                .contents()
                .forEach(object -> {
                    if (object.key().endsWith(suffix)) {
                        matches.add(object);
                    }
                });

        if (matches.size() > 1) {
            log.warn("같은 거래일의 리포트가 여러 개입니다. 최신을 씁니다. prefix={} 개수={}",
                    prefix, matches.size());
        }

        // lastModified가 같을 수 있어 key를 보조 기준으로 둔다. 실행마다 다른 걸 고르면 안 된다.
        return matches.stream()
                .max(Comparator.comparing(S3Object::lastModified).thenComparing(S3Object::key));
    }

    public record RecoveryResult(int recovered, int lookupFailures) {
    }
}
