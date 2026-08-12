package com.example.demo.domain.stock.service;

import com.example.demo.api.stock.dto.StockSyncResultDto;
import com.example.demo.common.config.JpaAuditingConfig;
import com.example.demo.domain.stock.entity.Stock;
import com.example.demo.domain.stock.entity.StockPriceSnapshot;
import com.example.demo.domain.stock.repository.StockRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 코스피200 동기화의 H2 기반 테스트.
 * 편출 종목은 삭제되지 않고 비활성으로 남아야 한다 — 관심종목·변동성 기록이 stock_code를 참조하기 때문이다.
 */
@DataJpaTest
@Import({StockCommandServiceImpl.class, JpaAuditingConfig.class})
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:stocksync;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class StockCommandServiceImplTest {

    @Autowired
    private StockCommandService stockCommandService;

    @Autowired
    private StockRepository stockRepository;

    @Test
    void 신규_종목은_활성으로_저장된다() {
        StockSyncResultDto result = stockCommandService.syncStocks(source("005930", "삼성전자"));

        assertThat(result.addedCount()).isEqualTo(1);
        assertThat(stockRepository.findByStockCode("005930"))
                .get()
                .satisfies(s -> assertThat(s.getIsActive()).isTrue());
    }

    @Test
    void 편출된_종목은_삭제되지_않고_비활성으로_남는다() {
        stockCommandService.syncStocks(source("005930", "삼성전자", "000660", "SK하이닉스"));

        StockSyncResultDto result = stockCommandService.syncStocks(source("005930", "삼성전자"));

        assertThat(result.deactivatedCount()).isEqualTo(1);
        // 행 자체는 남아 있어야 한다
        assertThat(stockRepository.findAll()).hasSize(2);
        assertThat(stockRepository.findByStockCode("000660"))
                .get()
                .satisfies(s -> assertThat(s.getIsActive()).isFalse());
    }

    @Test
    void 재편입되면_다시_활성으로_되돌린다() {
        stockCommandService.syncStocks(source("005930", "삼성전자", "000660", "SK하이닉스"));
        stockCommandService.syncStocks(source("005930", "삼성전자"));                       // 편출
        StockSyncResultDto result = stockCommandService.syncStocks(
                source("005930", "삼성전자", "000660", "SK하이닉스"));                       // 재편입

        assertThat(result.reactivatedCount()).isEqualTo(1);
        assertThat(result.addedCount()).isZero();   // 새 행이 생기면 안 된다
        assertThat(stockRepository.findAll()).hasSize(2);
        assertThat(stockRepository.findByStockCode("000660"))
                .get()
                .satisfies(s -> assertThat(s.getIsActive()).isTrue());
    }

    @Test
    void 종목명이_바뀌면_갱신한다() {
        stockCommandService.syncStocks(source("005930", "삼성전자"));

        StockSyncResultDto result = stockCommandService.syncStocks(source("005930", "삼성전자우"));

        assertThat(result.updatedCount()).isEqualTo(1);
        assertThat(stockRepository.findByStockCode("005930"))
                .get()
                .satisfies(s -> assertThat(s.getStockName()).isEqualTo("삼성전자우"));
    }

    @Test
    void 활성_종목만_조회된다() {
        stockCommandService.syncStocks(source("005930", "삼성전자", "000660", "SK하이닉스"));
        stockCommandService.syncStocks(source("005930", "삼성전자"));

        assertThat(stockRepository.findAllByIsActiveTrue())
                .extracting(Stock::getStockCode)
                .containsExactly("005930");
    }

    @Test
    void 여러_종목의_시세를_한_번에_갱신한다() {
        stockCommandService.syncStocks(source("005930", "삼성전자", "000660", "SK하이닉스"));
        LocalDate tradeDate = LocalDate.of(2026, 8, 2);

        int updated = stockCommandService.updateStockPrices(List.of(
                new StockPriceSnapshot("005930", new BigDecimal("71000"), new BigDecimal("72500")),
                new StockPriceSnapshot("000660", new BigDecimal("180000"), new BigDecimal("183000"))
        ), tradeDate);

        assertThat(updated).isEqualTo(2);
        assertThat(stockRepository.findByStockCode("005930")).get().satisfies(s -> {
            assertThat(s.getOpenPrice()).isEqualByComparingTo("71000");
            assertThat(s.getClosePrice()).isEqualByComparingTo("72500");
            assertThat(s.getTradeDate()).isEqualTo(tradeDate);
        });
    }

    @Test
    void DB에_없는_종목의_시세는_건너뛴다() {
        stockCommandService.syncStocks(source("005930", "삼성전자"));

        int updated = stockCommandService.updateStockPrices(List.of(
                new StockPriceSnapshot("005930", new BigDecimal("71000"), new BigDecimal("72500")),
                new StockPriceSnapshot("999999", new BigDecimal("1000"), new BigDecimal("1100"))
        ), LocalDate.of(2026, 8, 2));

        assertThat(updated).isEqualTo(1);
        assertThat(stockRepository.findAll()).hasSize(1);
    }

    @Test
    void 비활성_종목도_시세는_갱신된다() {
        stockCommandService.syncStocks(source("005930", "삼성전자", "000660", "SK하이닉스"));
        stockCommandService.syncStocks(source("005930", "삼성전자"));   // 000660 편출

        int updated = stockCommandService.updateStockPrices(List.of(
                new StockPriceSnapshot("000660", new BigDecimal("180000"), new BigDecimal("183000"))
        ), LocalDate.of(2026, 8, 2));

        assertThat(updated).isEqualTo(1);
    }

    private Map<String, String> source(String... codeAndName) {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < codeAndName.length; i += 2) {
            map.put(codeAndName[i], codeAndName[i + 1]);
        }
        return map;
    }
}
