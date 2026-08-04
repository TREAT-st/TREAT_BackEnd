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

    @Autowired
    private VolatilityCommandService volatilityCommandService;

    @Autowired
    private VolatilityRepository volatilityRepository;

    @Test
    void top10_종목의_코드와_이름만_저장되고_reportUrl은_null() {
        volatilityCommandService.saveTopVolatilityStocks(List.of(
                signal("005930", "삼성전자"),
                signal("000660", "SK하이닉스")
        ));

        List<Volatility> saved = volatilityRepository.findAll();

        assertThat(saved).hasSize(2);
        assertThat(saved).extracting(Volatility::getStockCode)
                .containsExactlyInAnyOrder("005930", "000660");
        assertThat(saved).extracting(Volatility::getStockName)
                .containsExactlyInAnyOrder("삼성전자", "SK하이닉스");
        assertThat(saved).allSatisfy(v -> assertThat(v.getReportUrl()).isNull());
    }

    @Test
    void createdDate가_자동으로_채워져_날짜별_조회가_가능하다() {
        volatilityCommandService.saveTopVolatilityStocks(List.of(signal("005930", "삼성전자")));

        List<Volatility> saved = volatilityRepository.findAll();

        assertThat(saved).singleElement()
                .satisfies(v -> assertThat(v.getCreatedDate()).isNotNull());
    }

    @Test
    void 같은_종목이_다시_선정되면_새_row로_쌓인다() {
        volatilityCommandService.saveTopVolatilityStocks(List.of(signal("005930", "삼성전자")));
        volatilityCommandService.saveTopVolatilityStocks(List.of(signal("005930", "삼성전자")));

        assertThat(volatilityRepository.findAllByStockCodeOrderByCreatedDateDesc("005930")).hasSize(2);
    }

    @Test
    void 빈_목록이면_아무것도_저장하지_않는다() {
        volatilityCommandService.saveTopVolatilityStocks(List.of());

        assertThat(volatilityRepository.findAll()).isEmpty();
    }

    private VolatilitySignal signal(String stockCode, String stockName) {
        return new VolatilitySignal(
                stockCode, stockName, 1,
                0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                0.5, true, List.of()
        );
    }
}
