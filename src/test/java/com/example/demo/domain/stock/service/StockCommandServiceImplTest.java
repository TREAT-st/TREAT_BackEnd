package com.example.demo.domain.stock.service;

import com.example.demo.common.config.JpaAuditingConfig;
import com.example.demo.domain.stock.entity.Kospi200SyncCommand;
import com.example.demo.domain.stock.entity.StockSyncOutcome;
import com.example.demo.domain.stock.entity.Stock;
import com.example.demo.domain.stock.entity.StockSyncResult;
import com.example.demo.domain.stock.entity.StockPriceSnapshot;
import com.example.demo.domain.stock.repository.StockRepository;
import com.example.demo.common.exception.GeneralException;
import com.example.demo.domain.stock.exception.StockErrorStatus;
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
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
        StockSyncResult result = stockCommandService.syncStocks(source("005930", "삼성전자"), Set.of());

        assertThat(result.addedCount()).isEqualTo(1);
        assertThat(stockRepository.findByStockCode("005930"))
                .get()
                .satisfies(s -> assertThat(s.getIsActive()).isTrue());
    }

    @Test
    void 편출된_종목은_삭제되지_않고_비활성으로_남는다() {
        stockCommandService.syncStocks(source("005930", "삼성전자", "000660", "SK하이닉스"), Set.of());

        StockSyncResult result = stockCommandService.syncStocks(source("005930", "삼성전자"), Set.of());

        assertThat(result.deactivatedCount()).isEqualTo(1);
        // 행 자체는 남아 있어야 한다
        assertThat(stockRepository.findAll()).hasSize(2);
        assertThat(stockRepository.findByStockCode("000660"))
                .get()
                .satisfies(s -> assertThat(s.getIsActive()).isFalse());
    }

    @Test
    void 재편입되면_다시_활성으로_되돌린다() {
        stockCommandService.syncStocks(source("005930", "삼성전자", "000660", "SK하이닉스"), Set.of());
        stockCommandService.syncStocks(source("005930", "삼성전자"), Set.of());                       // 편출
        StockSyncResult result = stockCommandService.syncStocks(
                source("005930", "삼성전자", "000660", "SK하이닉스"), Set.of());             // 재편입

        assertThat(result.reactivatedCount()).isEqualTo(1);
        assertThat(result.addedCount()).isZero();   // 새 행이 생기면 안 된다
        assertThat(stockRepository.findAll()).hasSize(2);
        assertThat(stockRepository.findByStockCode("000660"))
                .get()
                .satisfies(s -> assertThat(s.getIsActive()).isTrue());
    }

    @Test
    void 시세를_못_받은_구성종목은_편출로_처리하지_않는다() {
        stockCommandService.syncStocks(source("005930", "삼성전자", "000660", "SK하이닉스"), Set.of());

        // 000660은 여전히 구성종목이지만 거래정지 등으로 시세를 못 받아 sourceStocks에서 빠진 상황
        StockSyncResult result = stockCommandService.syncStocks(
                source("005930", "삼성전자"), Set.of("000660"));

        assertThat(result.deactivatedCount()).isZero();
        assertThat(stockRepository.findByStockCode("000660"))
                .get()
                .satisfies(s -> assertThat(s.getIsActive()).isTrue());
    }

    @Test
    void 시세를_못_받았어도_보류_목록에_없으면_편출_처리한다() {
        stockCommandService.syncStocks(source("005930", "삼성전자", "000660", "SK하이닉스"), Set.of());

        // 보류 목록에 다른 종목만 있는 경우 — 000660은 정상적으로 편출 판정
        StockSyncResult result = stockCommandService.syncStocks(
                source("005930", "삼성전자"), Set.of("999999"));

        assertThat(result.deactivatedCount()).isEqualTo(1);
        assertThat(stockRepository.findByStockCode("000660"))
                .get()
                .satisfies(s -> assertThat(s.getIsActive()).isFalse());
    }

    @Test
    void 종목명이_바뀌면_갱신한다() {
        stockCommandService.syncStocks(source("005930", "삼성전자"), Set.of());

        StockSyncResult result = stockCommandService.syncStocks(source("005930", "삼성전자우"), Set.of());

        assertThat(result.updatedCount()).isEqualTo(1);
        assertThat(stockRepository.findByStockCode("005930"))
                .get()
                .satisfies(s -> assertThat(s.getStockName()).isEqualTo("삼성전자우"));
    }

    @Test
    void 활성_비활성_종목을_구분해서_조회한다() {
        stockCommandService.syncStocks(source("005930", "삼성전자", "000660", "SK하이닉스"), Set.of());
        stockCommandService.syncStocks(source("005930", "삼성전자"), Set.of());   // 000660 편출

        assertThat(stockRepository.findAll())
                .filteredOn(stock -> Boolean.TRUE.equals(stock.getIsActive()))
                .extracting(Stock::getStockCode)
                .containsExactly("005930");
        assertThat(stockRepository.findAll())
                .filteredOn(stock -> !Boolean.TRUE.equals(stock.getIsActive()))
                .extracting(Stock::getStockCode)
                .containsExactly("000660");
        assertThat(stockRepository.findAll())
                .extracting(Stock::getStockCode)
                .containsExactlyInAnyOrder("005930", "000660");
    }

    /**
     * 응답에 없는 종목이 편출인지 일시적 실패인지는 DB와 비교해야만 갈린다.
     * errors에 있으면 보류(활성 유지), 없으면 편출.
     */
    @Test
    void 응답에_없는_종목은_errors_여부로_편출과_보류가_갈린다() {
        stockCommandService.syncStocks(
                source("005930", "삼성전자", "000660", "SK하이닉스", "035420", "NAVER"), Set.of());

        // 이번 응답에는 005930만 왔고, 000660은 Lambda가 실패 보고, 035420은 아무 소식 없음
        StockSyncResult result =
                stockCommandService.syncStocks(source("005930", "삼성전자"), Set.of("000660"));

        assertThat(result.unresolvedStockCodes()).containsExactly("000660");
        assertThat(result.deactivatedCount()).isEqualTo(1);

        assertThat(stockRepository.findByStockCode("000660")).get()
                .satisfies(s -> assertThat(s.getIsActive()).isTrue());
        assertThat(stockRepository.findByStockCode("035420")).get()
                .satisfies(s -> assertThat(s.getIsActive()).isFalse());
    }

    @Test
    void 신규_종목은_errors와_무관하게_추가된다() {
        StockSyncResult result = stockCommandService.syncStocks(
                source("005930", "삼성전자"), Set.of("999999"));

        assertThat(result.addedCount()).isEqualTo(1);
        // DB에 없던 종목은 보류할 상태 자체가 없으므로 목록에 잡히지 않는다.
        assertThat(result.unresolvedStockCodes()).isEmpty();
    }

    @Test
    void 여러_종목의_시세를_한_번에_갱신한다() {
        stockCommandService.syncStocks(source("005930", "삼성전자", "000660", "SK하이닉스"), Set.of());
        LocalDate tradeDate = LocalDate.of(2026, 8, 2);

        var priceResult = stockCommandService.updateStockPrices(List.of(
                new StockPriceSnapshot("005930", new BigDecimal("71000"), new BigDecimal("72500")),
                new StockPriceSnapshot("000660", new BigDecimal("180000"), new BigDecimal("183000"))
        ), tradeDate);

        assertThat(priceResult.updatedCount()).isEqualTo(2);
        assertThat(stockRepository.findByStockCode("005930")).get().satisfies(s -> {
            assertThat(s.getOpenPrice()).isEqualByComparingTo("71000");
            assertThat(s.getClosePrice()).isEqualByComparingTo("72500");
            assertThat(s.getTradeDate()).isEqualTo(tradeDate);
        });
    }

    /**
     * syncStocksAndPrices는 목록 저장 뒤에 시세를 갱신한다.
     * 신규 종목의 insert가 flush되지 않으면 시세 조회에서 안 잡혀 첫날 시세가 통째로 비게 되므로,
     * 같은 실행 안에서 반영되는지를 고정한다.
     */
    /**
     * 지수 구성종목 목록이 일부만 조회되면 빠진 종목이 전부 편출로 판정된다.
     * "목록이 비었을 때"만 막는 가드로는 못 잡으므로, 규모로 한 번 더 막는다.
     */
    @Test
    void 편출이_비정상적으로_많으면_동기화를_중단한다() {
        stockCommandService.syncStocks(manyStocks(30), Set.of());

        // 30종목 중 1종목만 수신 → 29종목이 편출 판정
        assertThatThrownBy(() -> stockCommandService.syncStocks(source("000001", "종목1"), Set.of()))
                .isInstanceOf(GeneralException.class)
                .extracting(e -> ((GeneralException) e).getErrorReasonHttpStatus().getCode())
                .isEqualTo(StockErrorStatus.STOCK_ABNORMAL_DEACTIVATION.getCode());
    }

    @Test
    void 정상_범위의_편출은_그대로_반영한다() {
        stockCommandService.syncStocks(manyStocks(30), Set.of());

        // 30종목 중 25종목 수신 → 5종목 편출. 정기변경 수준이라 통과해야 한다.
        StockSyncResult result = stockCommandService.syncStocks(manyStocks(25), Set.of());

        assertThat(result.deactivatedCount()).isEqualTo(5);
    }

    @Test
    void 대량_이탈이어도_errors로_보류되면_중단하지_않는다() {
        stockCommandService.syncStocks(manyStocks(30), Set.of());

        // 29종목이 목록에서 빠졌지만 전부 Lambda가 실패 보고한 것이므로 편출이 아니다.
        Set<String> allButFirst = manyStocks(30).keySet().stream()
                .filter(code -> !code.equals("000001"))
                .collect(java.util.stream.Collectors.toSet());
        StockSyncResult result = stockCommandService.syncStocks(source("000001", "종목1"), allButFirst);

        assertThat(result.deactivatedCount()).isZero();
        assertThat(result.unresolvedStockCodes()).hasSize(29);
    }

    private Map<String, String> manyStocks(int count) {
        Map<String, String> stocks = new java.util.LinkedHashMap<>();
        for (int i = 1; i <= count; i++) {
            stocks.put(String.format("%06d", i), "종목" + i);
        }
        return stocks;
    }

    @Test
    void 신규_종목의_시세도_같은_실행에서_반영된다() {
        LocalDate tradeDate = LocalDate.of(2026, 8, 2);

        StockSyncOutcome outcome = stockCommandService.syncStocksAndPrices(new Kospi200SyncCommand(
                tradeDate,
                source("005930", "삼성전자"),
                Set.of(),
                List.of(new StockPriceSnapshot("005930", new BigDecimal("71000"), new BigDecimal("72500"))),
                List.of()));

        assertThat(outcome.syncResult().addedCount()).isEqualTo(1);
        assertThat(outcome.priceUpdatedCount()).isEqualTo(1);
        assertThat(outcome.priceUpdateSkippedStockCodes()).isEmpty();
        assertThat(stockRepository.findByStockCode("005930")).get()
                .satisfies(s -> assertThat(s.getClosePrice()).isEqualByComparingTo("72500"));
    }

    @Test
    void DB에_없는_종목의_시세는_건너뛴다() {
        stockCommandService.syncStocks(source("005930", "삼성전자"), Set.of());

        var priceResult = stockCommandService.updateStockPrices(List.of(
                new StockPriceSnapshot("005930", new BigDecimal("71000"), new BigDecimal("72500")),
                new StockPriceSnapshot("999999", new BigDecimal("1000"), new BigDecimal("1100"))
        ), LocalDate.of(2026, 8, 2));

        assertThat(priceResult.updatedCount()).isEqualTo(1);
        // 건수만으로는 어느 종목이 빠졌는지 알 수 없어 추적이 안 된다. 코드까지 돌려준다.
        assertThat(priceResult.skippedStockCodes()).containsExactly("999999");
        assertThat(stockRepository.findAll()).hasSize(1);
    }

    @Test
    void 편출된_종목의_시세는_갱신하지_않는다() {
        stockCommandService.syncStocks(source("005930", "삼성전자", "000660", "SK하이닉스"), Set.of());
        stockCommandService.syncStocks(source("005930", "삼성전자"), Set.of());   // 000660 편출

        var priceResult = stockCommandService.updateStockPrices(List.of(
                new StockPriceSnapshot("005930", new BigDecimal("71000"), new BigDecimal("72500")),
                new StockPriceSnapshot("000660", new BigDecimal("180000"), new BigDecimal("183000"))
        ), LocalDate.of(2026, 8, 2));

        assertThat(priceResult.updatedCount()).isEqualTo(1);
        assertThat(priceResult.skippedStockCodes()).containsExactly("000660");
        assertThat(stockRepository.findByStockCode("000660")).get().satisfies(s -> {
            assertThat(s.getOpenPrice()).isNull();
            assertThat(s.getClosePrice()).isNull();
            assertThat(s.getTradeDate()).isNull();
        });
    }

    private Map<String, String> source(String... codeAndName) {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < codeAndName.length; i += 2) {
            map.put(codeAndName[i], codeAndName[i + 1]);
        }
        return map;
    }
}
