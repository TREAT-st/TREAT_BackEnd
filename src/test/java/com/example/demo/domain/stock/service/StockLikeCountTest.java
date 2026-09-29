package com.example.demo.domain.stock.service;

import com.example.demo.common.config.JpaAuditingConfig;
import com.example.demo.domain.favoriteStock.repository.FavoriteStockRepository;
import com.example.demo.domain.stock.entity.Stock;
import com.example.demo.domain.stock.repository.StockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import jakarta.persistence.EntityManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 관심등록수 증감 검증.
 * 엔티티를 읽어 +1 하는 방식은 동시 등록에서 갱신이 유실되므로 DB UPDATE로 처리한다.
 */
@DataJpaTest
@Import({StockCommandServiceImpl.class, JpaAuditingConfig.class})
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:stocklike;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class StockLikeCountTest {

    @Autowired
    private StockCommandService stockCommandService;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private FavoriteStockRepository favoriteStockRepository;

    @Autowired
    private EntityManager em;

    @BeforeEach
    void setUp() {
        stockRepository.save(Stock.builder().stockCode("005930").stockName("삼성전자").build());
        stockRepository.flush();
    }

    /** @Modifying 쿼리는 영속성 컨텍스트를 우회하므로, 캐시된 엔티티가 아니라 DB 값을 읽어야 한다. */
    private Long likeCountOf(String stockCode) {
        em.flush();
        em.clear();
        return stockRepository.findByStockCode(stockCode).orElseThrow().getLikeCount();
    }

    @Test
    void 기본값은_0이다() {
        assertThat(likeCountOf("005930")).isZero();
    }

    @Test
    void 관심등록하면_1_오른다() {
        stockCommandService.increaseLikeCount("005930");

        assertThat(likeCountOf("005930")).isEqualTo(1L);
    }

    @Test
    void 여러_사용자의_등록이_누적된다() {
        stockCommandService.increaseLikeCount("005930");
        stockCommandService.increaseLikeCount("005930");
        stockCommandService.increaseLikeCount("005930");

        assertThat(likeCountOf("005930")).isEqualTo(3L);
    }

    @Test
    void 해제하면_1_내려간다() {
        stockCommandService.increaseLikeCount("005930");
        stockCommandService.increaseLikeCount("005930");

        stockCommandService.decreaseLikeCount("005930");

        assertThat(likeCountOf("005930")).isEqualTo(1L);
    }

    /** 집계가 어긋나도 음수로 내려가면 안 된다. */
    @Test
    void 카운트가_0이면_더_내려가지_않는다() {
        stockCommandService.decreaseLikeCount("005930");
        stockCommandService.decreaseLikeCount("005930");

        assertThat(likeCountOf("005930")).isZero();
    }

    /** FavoriteStock.stockCode는 FK가 아니라 stock에 없는 코드가 올 수 있다. 예외 없이 넘어가야 한다. */
    @Test
    void stock에_없는_종목이면_아무것도_하지_않는다() {
        stockCommandService.increaseLikeCount("999999");
        stockCommandService.decreaseLikeCount("999999");

        assertThat(stockRepository.findByStockCode("999999")).isEmpty();
        assertThat(likeCountOf("005930")).isZero();
    }

    @Test
    void 다른_종목의_카운트는_건드리지_않는다() {
        stockRepository.save(Stock.builder().stockCode("000660").stockName("SK하이닉스").build());
        stockRepository.flush();

        stockCommandService.increaseLikeCount("005930");

        assertThat(likeCountOf("005930")).isEqualTo(1L);
        assertThat(likeCountOf("000660")).isZero();
    }

    /**
     * 증감이 어긋났을 때 관심종목 테이블을 기준으로 다시 세는 복구 경로.
     *
     * H2에서 user가 예약어라 User 엔티티를 저장할 수 없어(favorite_stock.user_id는 NOT NULL + FK)
     * 관심종목 행이 있는 경우는 여기서 재현할 수 없다. 관심종목이 0건일 때 카운트가
     * 실제값인 0으로 되돌아가는지까지 검증한다 — 상관 서브쿼리가 실제로 실행되고 반영되는지는 확인된다.
     */
    @Test
    void 관심종목_테이블로_카운트를_다시_센다() {
        stockCommandService.increaseLikeCount("005930");
        stockCommandService.increaseLikeCount("005930");
        assertThat(likeCountOf("005930")).isEqualTo(2L);

        assertThat(favoriteStockRepository.count()).isZero();
        stockRepository.recalculateLikeCounts();

        assertThat(likeCountOf("005930")).isZero();
    }
}
