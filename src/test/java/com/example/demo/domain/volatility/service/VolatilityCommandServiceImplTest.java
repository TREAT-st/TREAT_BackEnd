package com.example.demo.domain.volatility.service;

import com.example.demo.common.config.JpaAuditingConfig;
import com.example.demo.domain.volatility.entity.Volatility;
import com.example.demo.domain.volatility.entity.VolatilitySignal;
import com.example.demo.domain.volatility.repository.VolatilityRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 알림 종목 저장 로직의 H2 기반 테스트.
 * application.yml이 요구하는 외부 DATABASE_* 환경변수를 쓰지 않도록 H2 설정을 직접 주입한다.
 */
@DataJpaTest
@Import({VolatilityCommandServiceImpl.class, JpaAuditingConfig.class})
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:volatility;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class VolatilityCommandServiceImplTest {

    private static final LocalDate TRADE_DATE = LocalDate.of(2026, 8, 14);

    @Autowired
    private VolatilityCommandService volatilityCommandService;

    @Autowired
    private VolatilityRepository volatilityRepository;

    @Test
    void top10_종목의_코드와_이름만_저장되고_reportUrl은_null() {
        volatilityCommandService.saveTopVolatilityStocks(List.of(
                signal("005930", "삼성전자"),
                signal("000660", "SK하이닉스")
        ), TRADE_DATE);

        List<Volatility> saved = volatilityRepository.findAll();

        assertThat(saved).hasSize(2);
        assertThat(saved).extracting(Volatility::getStockCode)
                .containsExactlyInAnyOrder("005930", "000660");
        assertThat(saved).extracting(Volatility::getStockName)
                .containsExactlyInAnyOrder("삼성전자", "SK하이닉스");
        assertThat(saved).allSatisfy(v -> assertThat(v.getReportUrl()).isNull());
        assertThat(saved).allSatisfy(v -> assertThat(v.getTradeDate()).isEqualTo(TRADE_DATE));
    }

    @Test
    void createdDate가_자동으로_채워진다() {
        volatilityCommandService.saveTopVolatilityStocks(List.of(signal("005930", "삼성전자")), TRADE_DATE);

        assertThat(volatilityRepository.findAll()).singleElement()
                .satisfies(v -> assertThat(v.getCreatedDate()).isNotNull());
    }

    @Test
    void 같은_거래일에_다시_실행하면_누적되지_않고_교체된다() {
        volatilityCommandService.saveTopVolatilityStocks(List.of(signal("005930", "삼성전자")), TRADE_DATE);
        volatilityCommandService.saveTopVolatilityStocks(List.of(signal("005930", "삼성전자")), TRADE_DATE);

        assertThat(volatilityRepository.findAllByStockCodeOrderByTradeDateDesc("005930")).hasSize(1);
    }

    @Test
    void 재실행으로_상위_목록에서_빠진_종목은_남지_않는다() {
        volatilityCommandService.saveTopVolatilityStocks(List.of(
                signal("005930", "삼성전자"),
                signal("000660", "SK하이닉스")
        ), TRADE_DATE);
        volatilityCommandService.saveTopVolatilityStocks(List.of(signal("035420", "NAVER")), TRADE_DATE);

        assertThat(volatilityRepository.findAllByTradeDateOrderByIdAsc(TRADE_DATE))
                .extracting(Volatility::getStockCode)
                .containsExactly("035420");
    }

    /**
     * 이번 수정의 핵심 회귀 테스트.
     * 이전 구현은 당일 기록을 전량 삭제 후 재삽입해서, 콜백으로 채워진 reportUrl이 통째로 사라졌다.
     */
    @Test
    void 같은_거래일_재실행에도_이미_생성된_reportUrl은_보존된다() {
        volatilityCommandService.saveTopVolatilityStocks(List.of(signal("005930", "삼성전자")), TRADE_DATE);
        volatilityCommandService.updateReportUrl("005930", TRADE_DATE, "https://example.com/report.html");

        volatilityCommandService.saveTopVolatilityStocks(List.of(signal("005930", "삼성전자")), TRADE_DATE);

        assertThat(volatilityRepository.findByStockCodeAndTradeDate("005930", TRADE_DATE))
                .get()
                .extracting(Volatility::getReportUrl)
                .isEqualTo("https://example.com/report.html");
    }

    @Test
    void 빈_목록이면_기존_기록을_지우지_않는다() {
        volatilityCommandService.saveTopVolatilityStocks(List.of(signal("005930", "삼성전자")), TRADE_DATE);

        volatilityCommandService.saveTopVolatilityStocks(List.of(), TRADE_DATE);

        assertThat(volatilityRepository.findAllByTradeDateOrderByIdAsc(TRADE_DATE)).hasSize(1);
    }

    @Test
    void 빈_목록이면_아무것도_저장하지_않는다() {
        volatilityCommandService.saveTopVolatilityStocks(List.of(), TRADE_DATE);

        assertThat(volatilityRepository.findAll()).isEmpty();
    }

    @Test
    void 거래일이_다르면_같은_종목도_별개로_쌓인다() {
        volatilityCommandService.saveTopVolatilityStocks(
                List.of(signal("005930", "삼성전자")), LocalDate.of(2026, 8, 13));
        volatilityCommandService.saveTopVolatilityStocks(
                List.of(signal("005930", "삼성전자")), LocalDate.of(2026, 8, 14));

        assertThat(volatilityRepository.findAllByStockCodeOrderByTradeDateDesc("005930"))
                .extracting(Volatility::getTradeDate)
                .containsExactly(LocalDate.of(2026, 8, 14), LocalDate.of(2026, 8, 13));
    }

    private VolatilitySignal signal(String stockCode, String stockName) {
        return new VolatilitySignal(
                stockCode, stockName, 1,
                0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                0.5, true, List.of()
        );
    }
}
