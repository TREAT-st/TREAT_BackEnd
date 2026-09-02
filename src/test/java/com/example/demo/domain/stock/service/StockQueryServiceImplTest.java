package com.example.demo.domain.stock.service;

import com.example.demo.common.config.JpaAuditingConfig;
import com.example.demo.domain.stock.entity.Stock;
import com.example.demo.domain.stock.repository.StockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * 종목 조회의 isActive 필터 검증.
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

    @Autowired
    private StockQueryService stockQueryService;

    @Autowired
    private StockRepository stockRepository;

    @BeforeEach
    void setUp() {
        stockRepository.deleteAll();
        stockRepository.save(Stock.builder().stockCode("005930").stockName("삼성전자").build());

        Stock delisted = Stock.builder().stockCode("000660").stockName("SK하이닉스").build();
        delisted.deactivate();
        stockRepository.save(delisted);
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
}
