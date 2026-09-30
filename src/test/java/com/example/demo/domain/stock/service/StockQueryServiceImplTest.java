package com.example.demo.domain.stock.service;

import com.example.demo.common.config.JpaAuditingConfig;
import com.example.demo.domain.stock.entity.Stock;
import com.example.demo.domain.stock.entity.StockSortType;
import com.example.demo.domain.stock.repository.StockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * 종목 조회의 거래일·isActive 필터와 정렬 검증.
 * isActive는 선택 파라미터라 null이 정상 입력이며, 이때 전체가 조회돼야 한다.
 */
@DataJpaTest
@Import({StockQueryServiceImpl.class, JpaAuditingConfig.class})
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:stockquery;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class StockQueryServiceImplTest {

    private static final Pageable PAGE = PageRequest.of(0, 20);
    private static final LocalDate TRADE_DATE = LocalDate.of(2026, 8, 2);

    @Autowired
    private StockQueryService stockQueryService;

    @Autowired
    private StockRepository stockRepository;

    @BeforeEach
    void setUp() {
        stockRepository.deleteAll();
        stockRepository.flush();
        stockRepository.save(Stock.builder().stockCode("005930").stockName("삼성전자").build());

        Stock delisted = Stock.builder().stockCode("000660").stockName("SK하이닉스").build();
        delisted.deactivate();
        stockRepository.save(delisted);
    }

    private Pageable pageSortedBy(StockSortType sortType) {
        return PageRequest.of(0, 20, sortType.getSort());
    }

    private Stock priced(String stockCode, String stockName, long marketCap, long likeCount) {
        Stock stock = Stock.builder()
                .stockCode(stockCode)
                .stockName(stockName)
                .likeCount(likeCount)
                .build();
        stock.updatePrice(new BigDecimal("1000"), new BigDecimal("1100"), marketCap, TRADE_DATE);
        return stock;
    }

    @Test
    void isActive가_null이면_예외없이_전체를_조회한다() {
        assertThatCode(() -> stockQueryService.getAllStocks(null, PAGE))
                .doesNotThrowAnyException();

        assertThat(stockQueryService.getAllStocks(null, PAGE))
                .extracting(Stock::getStockCode)
                .containsExactlyInAnyOrder("005930", "000660");
    }

    @Test
    void isActive가_true면_편입_종목만_조회한다() {
        assertThat(stockQueryService.getAllStocks(true, PAGE))
                .extracting(Stock::getStockCode)
                .containsExactly("005930");
    }

    @Test
    void isActive가_false면_전체를_조회한다() {
        assertThat(stockQueryService.getAllStocks(false, PAGE))
                .extracting(Stock::getStockCode)
                .containsExactlyInAnyOrder("005930", "000660");
    }





    @Test
    void 시가총액_내림차순으로_정렬한다() {
        stockRepository.deleteAll();
        stockRepository.flush();
        stockRepository.save(priced("000660", "SK하이닉스", 133_000_000_000_000L, 0L));
        stockRepository.save(priced("005930", "삼성전자", 432_000_000_000_000L, 0L));
        stockRepository.save(priced("035720", "카카오", 25_000_000_000_000L, 0L));

        assertThat(stockQueryService.getAllStocks(null, pageSortedBy(StockSortType.MARKET_CAP)))
                .extracting(Stock::getStockCode)
                .containsExactly("005930", "000660", "035720");
    }

    @Test
    void 관심등록수_내림차순으로_정렬한다() {
        stockRepository.deleteAll();
        stockRepository.flush();
        stockRepository.save(priced("000660", "SK하이닉스", 133_000_000_000_000L, 5L));
        stockRepository.save(priced("005930", "삼성전자", 432_000_000_000_000L, 42L));
        stockRepository.save(priced("035720", "카카오", 25_000_000_000_000L, 17L));

        assertThat(stockQueryService.getAllStocks(null, pageSortedBy(StockSortType.LIKE_COUNT)))
                .extracting(Stock::getStockCode)
                .containsExactly("005930", "035720", "000660");
    }

    /**
     * likeCount는 초기에 전부 0이라 동점이 대량으로 생긴다.
     * 2차 키가 없으면 페이지 경계에서 순서가 흔들려 종목이 중복·누락된다.
     */
    @Test
    void 정렬값이_같으면_종목코드로_순서를_고정한다() {
        stockRepository.deleteAll();
        stockRepository.flush();
        stockRepository.save(priced("035720", "카카오", 25_000_000_000_000L, 0L));
        stockRepository.save(priced("005930", "삼성전자", 25_000_000_000_000L, 0L));
        stockRepository.save(priced("000660", "SK하이닉스", 25_000_000_000_000L, 0L));

        assertThat(stockQueryService.getAllStocks(null, pageSortedBy(StockSortType.LIKE_COUNT)))
                .extracting(Stock::getStockCode)
                .containsExactly("000660", "005930", "035720");
        assertThat(stockQueryService.getAllStocks(null, pageSortedBy(StockSortType.MARKET_CAP)))
                .extracting(Stock::getStockCode)
                .containsExactly("000660", "005930", "035720");
    }
}
