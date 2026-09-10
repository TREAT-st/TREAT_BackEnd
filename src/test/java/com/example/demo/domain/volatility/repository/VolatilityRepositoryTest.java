package com.example.demo.domain.volatility.repository;

import com.example.demo.common.config.JpaAuditingConfig;
import com.example.demo.domain.volatility.entity.Volatility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * S3 보정이 쓰는 조건부 갱신 검증.
 *
 * 목으로는 "비어 있을 때만"이 실제 SQL에서 지켜지는지 확인할 수 없다.
 * 이 조건이 새면 스위퍼가 콜백이 방금 채운 최신 URL을 오래된 판단으로 덮는다.
 */
@DataJpaTest
@Import(JpaAuditingConfig.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:volatilityrepo;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class VolatilityRepositoryTest {

    private static final LocalDate TRADE_DATE = LocalDate.of(2026, 9, 9);
    private static final String CALLBACK_URL = "https://example.com/from-callback_analysis.html";
    private static final String S3_URL = "https://example.com/from-s3_analysis.html";

    @Autowired
    private VolatilityRepository volatilityRepository;

    @BeforeEach
    void clear() {
        volatilityRepository.deleteAll();
    }

    @Test
    void 비어_있으면_채운다() {
        save("000660", null);

        int updated = volatilityRepository.fillReportUrlIfAbsent("000660", TRADE_DATE, S3_URL);

        assertThat(updated).isEqualTo(1);
        assertThat(reportUrlOf("000660")).isEqualTo(S3_URL);
    }

    /** 콜백이 먼저 채웠다면 그쪽이 더 최신이다. 갱신 0건이 올바른 결과다. */
    @Test
    void 이미_있으면_덮지_않고_값을_보존한다() {
        save("000660", CALLBACK_URL);

        int updated = volatilityRepository.fillReportUrlIfAbsent("000660", TRADE_DATE, S3_URL);

        assertThat(updated).isZero();
        assertThat(reportUrlOf("000660")).isEqualTo(CALLBACK_URL);
    }

    @Test
    void 다른_거래일은_건드리지_않는다() {
        save("000660", null);
        save("000660", null, TRADE_DATE.minusDays(1));

        volatilityRepository.fillReportUrlIfAbsent("000660", TRADE_DATE, S3_URL);

        assertThat(reportUrlOf("000660")).isEqualTo(S3_URL);
        assertThat(volatilityRepository.findByStockCodeAndTradeDate("000660", TRADE_DATE.minusDays(1))
                .orElseThrow().getReportUrl()).isNull();
    }

    @Test
    void 없는_종목이면_아무것도_갱신하지_않는다() {
        int updated = volatilityRepository.fillReportUrlIfAbsent("999999", TRADE_DATE, S3_URL);

        assertThat(updated).isZero();
    }

    @Test
    void 미도착_종목만_조회한다() {
        save("000660", null);
        save("005930", CALLBACK_URL);

        assertThat(volatilityRepository.findAllByTradeDateAndReportUrlIsNull(TRADE_DATE))
                .extracting(Volatility::getStockCode)
                .containsExactly("000660");
        assertThat(volatilityRepository.countByTradeDateAndReportUrlIsNull(TRADE_DATE)).isEqualTo(1);
    }

    private String reportUrlOf(String stockCode) {
        return volatilityRepository.findByStockCodeAndTradeDate(stockCode, TRADE_DATE)
                .orElseThrow().getReportUrl();
    }

    private void save(String stockCode, String reportUrl) {
        save(stockCode, reportUrl, TRADE_DATE);
    }

    private void save(String stockCode, String reportUrl, LocalDate tradeDate) {
        volatilityRepository.saveAndFlush(Volatility.builder()
                .stockCode(stockCode)
                .stockName("종목")
                .tradeDate(tradeDate)
                .reportUrl(reportUrl)
                .build());
    }
}
