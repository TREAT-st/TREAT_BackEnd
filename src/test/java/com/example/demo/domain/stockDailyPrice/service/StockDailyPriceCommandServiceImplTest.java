package com.example.demo.domain.stockDailyPrice.service;

import com.example.demo.common.config.JpaAuditingConfig;
import com.example.demo.domain.stockDailyPrice.entity.DailyPriceRow;
import com.example.demo.domain.stockDailyPrice.entity.DailyPriceSyncResult;
import com.example.demo.domain.stockDailyPrice.entity.DailyPriceSyncStatus;
import com.example.demo.domain.stockDailyPrice.entity.StockDailyPrice;
import com.example.demo.domain.stockDailyPrice.entity.StockDailyPriceDeleteResult;
import com.example.demo.domain.stockDailyPrice.entity.StockDailyPriceSyncLog;
import com.example.demo.domain.stockDailyPrice.repository.StockDailyPriceJobLockRepository;
import com.example.demo.domain.stockDailyPrice.repository.StockDailyPriceRepository;
import com.example.demo.domain.stockDailyPrice.repository.StockDailyPriceSyncLogRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** 일봉 upsert의 H2 기반 테스트. 매일 동기화와의 연동은 StockCommandServiceImplTest에서 본다. */
@DataJpaTest
@Import({StockDailyPriceCommandServiceImpl.class, JpaAuditingConfig.class})
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:stockdailyprice;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class StockDailyPriceCommandServiceImplTest {

    @Autowired
    private StockDailyPriceCommandService stockDailyPriceCommandService;

    @Autowired
    private StockDailyPriceRepository stockDailyPriceRepository;

    @Autowired
    private StockDailyPriceSyncLogRepository stockDailyPriceSyncLogRepository;

    @Autowired
    private StockDailyPriceJobLockRepository stockDailyPriceJobLockRepository;

    private List<StockDailyPrice> dailyPrices(String stockCode) {
        return stockDailyPriceRepository.findAllByStockCodeAndTradeDateBetweenOrderByTradeDateAsc(
                stockCode, LocalDate.of(2000, 1, 1), LocalDate.of(2100, 1, 1));
    }

    private DailyPriceRow row(String stockCode, LocalDate tradeDate, String close) {
        return new DailyPriceRow(stockCode, tradeDate,
                new BigDecimal("100"), new BigDecimal("120"), new BigDecimal("90"), new BigDecimal(close));
    }

    @Test
    void 여러_날짜를_한번에_넣고_다시_넣으면_갱신한다() {
        LocalDate d1 = LocalDate.of(2026, 10, 14);
        LocalDate d2 = LocalDate.of(2026, 10, 15);
        stockDailyPriceCommandService.saveDailyPrices(List.of(row("005930", d1, "100"), row("005930", d2, "110")));

        int saved = stockDailyPriceCommandService.saveDailyPrices(List.of(row("005930", d2, "115"), row("005930", LocalDate.of(2026, 10, 16), "120")));

        assertThat(saved).isEqualTo(2);
        assertThat(dailyPrices("005930")).extracting(d -> d.getClosePrice().intValue())
                .containsExactly(100, 115, 120);
    }

    /** 시가·종가는 필수, 고가·저가는 비어도 된다. 0이나 음수는 거래정지일 같은 가짜 시세다. */
    @Test
    void 잘못된_일봉은_버린다() {
        LocalDate date = LocalDate.of(2026, 10, 16);
        int saved = stockDailyPriceCommandService.saveDailyPrices(List.of(
                new DailyPriceRow("000001", date, null, null, null, new BigDecimal("100")),
                new DailyPriceRow("000002", date, new BigDecimal("100"), null, null, BigDecimal.ZERO),
                new DailyPriceRow("000003", date, new BigDecimal("100"), new BigDecimal("-1"), null, new BigDecimal("100")),
                new DailyPriceRow("000004", date, new BigDecimal("100"), null, null, new BigDecimal("105"))));

        assertThat(saved).isEqualTo(1);
        assertThat(stockDailyPriceRepository.findAll()).singleElement()
                .satisfies(d -> {
                    assertThat(d.getStockCode()).isEqualTo("000004");
                    assertThat(d.getHighPrice()).isNull();
                });
    }

    /** 한 요청 안에 같은 (종목, 날짜)가 두 번 오면 둘 다 새 행으로 넣다가 유니크 제약에 걸린다. 뒤의 값을 쓴다. */
    @Test
    void 요청_안의_중복은_마지막_값만_쓴다() {
        LocalDate date = LocalDate.of(2026, 10, 16);

        int saved = stockDailyPriceCommandService.saveDailyPrices(List.of(row("005930", date, "100"), row("005930", date, "105")));

        assertThat(saved).isEqualTo(1);
        assertThat(dailyPrices("005930")).singleElement()
                .satisfies(d -> assertThat(d.getClosePrice()).isEqualByComparingTo("105"));
    }

    // ---------- 적재 이력 ----------

    private DailyPriceSyncResult result(DailyPriceSyncStatus status, List<String> failed) {
        return new DailyPriceSyncResult(status, 200, 199, 0, failed,
                LocalDateTime.of(2026, 10, 17, 17, 0), LocalDateTime.of(2026, 10, 17, 17, 1), null);
    }

    @Test
    void 같은_거래일을_다시_기록하면_덮어쓰고_시도_횟수를_올린다() {
        LocalDate date = LocalDate.of(2026, 10, 16);
        stockDailyPriceCommandService.recordSyncResult(date, result(DailyPriceSyncStatus.PARTIAL, List.of("000660", "035420")));
        stockDailyPriceCommandService.recordSyncResult(date, result(DailyPriceSyncStatus.SUCCESS, List.of()));

        assertThat(stockDailyPriceSyncLogRepository.findAll()).singleElement().satisfies(log -> {
            assertThat(log.getStatus()).isEqualTo(DailyPriceSyncStatus.SUCCESS);
            assertThat(log.getAttempt()).isEqualTo(2);
            assertThat(log.failedStockCodeList()).isEmpty();
        });
    }

    @Test
    void 실패_종목은_목록으로_다시_읽힌다() {
        LocalDate date = LocalDate.of(2026, 10, 16);
        stockDailyPriceCommandService.recordSyncResult(date, result(DailyPriceSyncStatus.PARTIAL, List.of("000660", "035420")));

        assertThat(stockDailyPriceSyncLogRepository.findByTradeDate(date)).get()
                .satisfies(log -> assertThat(log.failedStockCodeList()).containsExactly("000660", "035420"));
    }

    /** 일일 적재의 시작점과 재시도 대상 쿼리. */
    @Test
    void 끝난_마지막_날짜와_재시도_대상을_가른다() {
        stockDailyPriceCommandService.recordSyncResult(LocalDate.of(2026, 10, 13), result(DailyPriceSyncStatus.SUCCESS, List.of()));
        stockDailyPriceCommandService.recordSyncResult(LocalDate.of(2026, 10, 14), result(DailyPriceSyncStatus.HOLIDAY, List.of()));
        stockDailyPriceCommandService.recordSyncResult(LocalDate.of(2026, 10, 15), result(DailyPriceSyncStatus.PARTIAL, List.of("000660")));
        LocalDate capped = LocalDate.of(2026, 10, 12);
        for (int i = 0; i < 5; i++) {
            stockDailyPriceCommandService.recordSyncResult(capped, result(DailyPriceSyncStatus.FAILED, List.of("005930")));
        }

        assertThat(stockDailyPriceSyncLogRepository.findLatestTradeDateByStatusIn(
                List.of(DailyPriceSyncStatus.SUCCESS, DailyPriceSyncStatus.HOLIDAY))).contains(LocalDate.of(2026, 10, 14));
        assertThat(stockDailyPriceSyncLogRepository.findAllByStatusInAndAttemptLessThanOrderByTradeDateAsc(
                List.of(DailyPriceSyncStatus.PARTIAL, DailyPriceSyncStatus.FAILED), 5))
                .extracting(StockDailyPriceSyncLog::getTradeDate)
                .containsExactly(LocalDate.of(2026, 10, 15));
    }

    // ---------- 삭제 ----------

    /** 이력은 성공인데 데이터가 없는 상태를 남기지 않는다. */
    @Test
    void 전_종목_삭제는_기간의_일봉과_이력을_함께_지운다() {
        stockDailyPriceCommandService.saveDailyPrices(List.of(
                row("005930", LocalDate.of(2026, 10, 14), "100"),
                row("000660", LocalDate.of(2026, 10, 15), "200"),
                row("005930", LocalDate.of(2026, 10, 16), "105")));
        stockDailyPriceCommandService.recordSyncResult(LocalDate.of(2026, 10, 15), result(DailyPriceSyncStatus.SUCCESS, List.of()));
        stockDailyPriceCommandService.recordSyncResult(LocalDate.of(2026, 10, 16), result(DailyPriceSyncStatus.SUCCESS, List.of()));

        StockDailyPriceDeleteResult deleted = stockDailyPriceCommandService.deleteDailyPrices(
                LocalDate.of(2026, 10, 14), LocalDate.of(2026, 10, 15), null);

        assertThat(deleted).isEqualTo(new StockDailyPriceDeleteResult(2, 1));
        assertThat(stockDailyPriceRepository.findAll()).extracting(StockDailyPrice::getTradeDate)
                .containsExactly(LocalDate.of(2026, 10, 16));
        assertThat(stockDailyPriceSyncLogRepository.findAll()).extracting(StockDailyPriceSyncLog::getTradeDate)
                .containsExactly(LocalDate.of(2026, 10, 16));
    }

    /** 이력은 날짜 단위라 한 종목 삭제를 표현할 수 없다. 그 종목은 백필로 다시 채운다. */
    @Test
    void 한_종목_삭제는_그_종목만_지우고_이력은_둔다() {
        LocalDate date = LocalDate.of(2026, 10, 16);
        stockDailyPriceCommandService.saveDailyPrices(List.of(row("005930", date, "100"), row("000660", date, "200")));
        stockDailyPriceCommandService.recordSyncResult(date, result(DailyPriceSyncStatus.SUCCESS, List.of()));

        StockDailyPriceDeleteResult deleted = stockDailyPriceCommandService.deleteDailyPrices(date, date, "005930");

        assertThat(deleted).isEqualTo(new StockDailyPriceDeleteResult(1, 0));
        assertThat(stockDailyPriceRepository.findAll()).extracting(StockDailyPrice::getStockCode).containsExactly("000660");
        assertThat(stockDailyPriceSyncLogRepository.findAll()).hasSize(1);
    }

    // ---------- 실행 잠금 ----------

    private static final String LOCK = "STOCK_DAILY_PRICE";
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 23, 19, 0);

    /** 서버 둘이 동시에 시도해도 하나만 잡는다. 조건부 UPDATE라 먼저 잡은 쪽만 1을 받는다. */
    @Test
    void 잠금은_한_실행만_잡는다() {
        stockDailyPriceCommandService.createJobLockIfAbsent(LOCK);

        assertThat(stockDailyPriceCommandService.tryAcquireJobLock(LOCK, "a", NOW, NOW.plusHours(1))).isTrue();
        assertThat(stockDailyPriceCommandService.tryAcquireJobLock(LOCK, "b", NOW.plusMinutes(5), NOW.plusHours(2))).isFalse();
    }

    @Test
    void 풀면_다른_실행이_잡는다() {
        stockDailyPriceCommandService.createJobLockIfAbsent(LOCK);
        stockDailyPriceCommandService.tryAcquireJobLock(LOCK, "a", NOW, NOW.plusHours(1));

        stockDailyPriceCommandService.releaseJobLock(LOCK, "a");

        assertThat(stockDailyPriceCommandService.tryAcquireJobLock(LOCK, "b", NOW, NOW.plusHours(1))).isTrue();
    }

    /** 다른 실행의 잠금은 풀지 않는다. 임대가 끝나 넘어간 뒤 늦게 끝난 이전 실행이 새 잠금을 풀면 안 된다. */
    @Test
    void 자기_잠금만_푼다() {
        stockDailyPriceCommandService.createJobLockIfAbsent(LOCK);
        stockDailyPriceCommandService.tryAcquireJobLock(LOCK, "a", NOW, NOW.plusHours(1));

        stockDailyPriceCommandService.releaseJobLock(LOCK, "b");

        assertThat(stockDailyPriceCommandService.tryAcquireJobLock(LOCK, "c", NOW, NOW.plusHours(1))).isFalse();
    }

    /** 서버가 작업 중에 죽으면 풀지 못한다. 임대가 끝나면 다른 실행이 가져간다. */
    @Test
    void 임대가_끝나면_다른_실행이_잡는다() {
        stockDailyPriceCommandService.createJobLockIfAbsent(LOCK);
        stockDailyPriceCommandService.tryAcquireJobLock(LOCK, "a", NOW, NOW.plusHours(1));

        assertThat(stockDailyPriceCommandService.tryAcquireJobLock(LOCK, "b", NOW.plusHours(1).plusSeconds(1), NOW.plusHours(2)))
                .isTrue();
    }

    @Test
    void 잠금_행은_한_번만_만든다() {
        stockDailyPriceCommandService.createJobLockIfAbsent(LOCK);
        stockDailyPriceCommandService.createJobLockIfAbsent(LOCK);

        assertThat(stockDailyPriceJobLockRepository.count()).isEqualTo(1);
    }

    @Test
    void 자기_잠금은_임대를_연장한다() {
        stockDailyPriceCommandService.createJobLockIfAbsent(LOCK);
        stockDailyPriceCommandService.tryAcquireJobLock(LOCK, "a", NOW, NOW.plusMinutes(15));

        assertThat(stockDailyPriceCommandService.extendJobLock(LOCK, "a", NOW.plusMinutes(30))).isTrue();
        // 연장됐으므로 원래 임대가 끝난 뒤(15분 후)에도 다른 실행은 못 가져간다
        assertThat(stockDailyPriceCommandService.tryAcquireJobLock(LOCK, "b", NOW.plusMinutes(20), NOW.plusMinutes(35)))
                .isFalse();
    }

    /** 임대가 끝나 다른 실행이 가져간 뒤라면 연장은 실패한다. 이전 실행은 이걸 보고 멈춘다. */
    @Test
    void 잠금을_잃었으면_연장하지_못한다() {
        stockDailyPriceCommandService.createJobLockIfAbsent(LOCK);
        stockDailyPriceCommandService.tryAcquireJobLock(LOCK, "a", NOW, NOW.plusMinutes(15));
        stockDailyPriceCommandService.tryAcquireJobLock(LOCK, "b", NOW.plusMinutes(16), NOW.plusMinutes(31));

        assertThat(stockDailyPriceCommandService.extendJobLock(LOCK, "a", NOW.plusMinutes(40))).isFalse();
    }
}
