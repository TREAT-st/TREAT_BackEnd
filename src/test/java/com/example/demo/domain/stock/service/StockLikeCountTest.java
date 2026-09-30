package com.example.demo.domain.stock.service;

import com.example.demo.common.config.JpaAuditingConfig;
import com.example.demo.domain.favoriteStock.entity.FavoriteStock;
import com.example.demo.domain.favoriteStock.repository.FavoriteStockRepository;
import com.example.demo.domain.user.entity.Role;
import com.example.demo.domain.user.entity.User;
import com.example.demo.domain.user.entity.UserStatus;
import com.example.demo.domain.stock.entity.Kospi200SyncCommand;
import com.example.demo.domain.stock.entity.Stock;
import com.example.demo.domain.stock.entity.StockPriceSnapshot;
import com.example.demo.domain.stock.repository.StockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import jakarta.persistence.EntityManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 관심등록수 증감 검증.
 * 엔티티를 읽어 +1 하는 방식은 동시 등록에서 갱신이 유실되므로 DB UPDATE로 처리한다.
 */
@DataJpaTest
// @DataJpaTest는 기본적으로 DataSource를 자체 임베디드 DB로 교체하며,
// 그러면 아래 URL의 MODE·NON_KEYWORDS 옵션이 무시된다.
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({StockCommandServiceImpl.class, JpaAuditingConfig.class})
@TestPropertySource(properties = {
        // NON_KEYWORDS=USER: H2에서 user가 예약어라 User 엔티티를 저장할 수 없다.
        // favorite_stock.user_id가 NOT NULL + FK라 관심종목 행을 만들려면 User가 필요하다.
        "spring.datasource.url=jdbc:h2:mem:stocklike;MODE=MySQL;DB_CLOSE_DELAY=-1;NON_KEYWORDS=USER",
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
     * 시세 동기화가 관심등록수를 되돌리지 않아야 한다.
     *
     * 동기화는 Stock을 읽어 두고 수정한다. 그 사이 관심등록이 들어오면 DB 값은 올라가지만
     * 엔티티에 담긴 값은 옛 값 그대로다. like_count가 엔티티 UPDATE에 포함되면
     * flush 때 옛 값이 DB를 덮어쓴다. updatable = false로 그 경로를 막았다.
     */
    @Test
    void 시세_동기화가_관심등록수를_덮어쓰지_않는다() {
        em.flush();
        em.clear();

        // 동기화가 Stock을 읽어 둔 시점 (엔티티의 likeCount = 0)
        Stock loaded = stockRepository.findByStockCode("005930").orElseThrow();
        assertThat(loaded.getLikeCount()).isZero();

        // 그 사이 다른 요청이 관심등록 → DB는 1, 엔티티는 여전히 0
        stockCommandService.increaseLikeCount("005930");

        // 동기화가 시세를 반영하고 flush
        loaded.updatePrice(new BigDecimal("71000"), new BigDecimal("72500"),
                432_000_000_000_000L, LocalDate.now(ZoneId.of("Asia/Seoul")).minusDays(1));
        em.flush();

        assertThat(likeCountOf("005930")).isEqualTo(1L);
    }

    /** syncStocksAndPrices 전체 경로에서도 같아야 한다. */
    @Test
    void 동기화_전체_경로에서도_관심등록수가_유지된다() {
        stockCommandService.increaseLikeCount("005930");
        stockCommandService.increaseLikeCount("005930");
        em.flush();
        em.clear();

        LocalDate tradeDate = LocalDate.now(ZoneId.of("Asia/Seoul")).minusDays(1);
        stockCommandService.syncStocksAndPrices(new Kospi200SyncCommand(
                tradeDate,
                Map.of("005930", "삼성전자"),
                Set.of(),
                List.of(new StockPriceSnapshot("005930",
                        new BigDecimal("71000"), new BigDecimal("72500"), 432_000_000_000_000L)),
                List.of()), false);

        assertThat(stockRepository.findByStockCode("005930")).get().satisfies(s -> {
            assertThat(s.getClosePrice()).isEqualByComparingTo("72500");
            assertThat(s.getTradeDate()).isEqualTo(tradeDate);
        });
        assertThat(likeCountOf("005930")).isEqualTo(2L);
    }

    private User persistUser() {
        User user = User.builder()
                .username(UUID.randomUUID().toString())
                .role(Role.values()[0])
                .status(UserStatus.ACTIVE)
                .build();
        em.persist(user);
        return user;
    }

    private void persistFavorite(User user, String stockCode, String stockName) {
        favoriteStockRepository.save(FavoriteStock.builder()
                .user(user).stockCode(stockCode).stockName(stockName).build());
    }

    /** 증감이 어긋났을 때 관심종목 테이블을 기준으로 다시 세는 복구 경로. */
    @Test
    void 관심종목_테이블로_카운트를_다시_센다() {
        stockRepository.save(Stock.builder().stockCode("000660").stockName("SK하이닉스").build());

        // 삼성전자에 3명, SK하이닉스에 1명
        User user1 = persistUser();
        User user2 = persistUser();
        User user3 = persistUser();
        persistFavorite(user1, "005930", "삼성전자");
        persistFavorite(user2, "005930", "삼성전자");
        persistFavorite(user3, "005930", "삼성전자");
        persistFavorite(user1, "000660", "SK하이닉스");
        favoriteStockRepository.flush();

        // 카운트가 실제와 어긋난 상태 — 삼성전자는 모자라고 SK하이닉스는 과다
        stockCommandService.increaseLikeCount("005930");
        stockCommandService.increaseLikeCount("000660");
        stockCommandService.increaseLikeCount("000660");
        stockCommandService.increaseLikeCount("000660");
        assertThat(likeCountOf("005930")).isEqualTo(1L);
        assertThat(likeCountOf("000660")).isEqualTo(3L);

        stockRepository.recalculateLikeCounts();

        assertThat(likeCountOf("005930")).isEqualTo(3L);
        assertThat(likeCountOf("000660")).isEqualTo(1L);
    }

    @Test
    void 관심종목이_없는_종목은_0으로_되돌린다() {
        stockCommandService.increaseLikeCount("005930");
        stockCommandService.increaseLikeCount("005930");
        assertThat(likeCountOf("005930")).isEqualTo(2L);

        assertThat(favoriteStockRepository.count()).isZero();
        stockRepository.recalculateLikeCounts();

        assertThat(likeCountOf("005930")).isZero();
    }
}
