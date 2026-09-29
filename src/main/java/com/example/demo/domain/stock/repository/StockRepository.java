package com.example.demo.domain.stock.repository;

import com.example.demo.domain.stock.entity.Stock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface StockRepository extends JpaRepository<Stock, Long> {
    Optional<Stock> findByStockCode(String stockCode);
    List<Stock> findAllByStockCodeInAndIsActiveTrue(Collection<String> stockCodes);
    Page<Stock> findAllByIsActive(Boolean isActive, Pageable pageable);

    /**
     * 시세가 반영된 가장 최근 거래일. 아직 아무 시세도 없으면 비어 있다.
     * Stock은 종목별 최신 스냅샷만 보관하므로 max(tradeDate)가 마지막 동기화 기준일이다.
     */
    @Query("select max(s.tradeDate) from Stock s")
    Optional<LocalDate> findLatestTradeDate();

    /**
     * 관심등록수를 DB에서 직접 증감한다.
     *
     * 엔티티를 읽어 +1 하면 두 사용자가 동시에 등록할 때 갱신이 유실된다(lost update).
     * DB가 행을 잠근 상태로 더하므로 그 창이 없다.
     *
     * 감소는 likeCount > 0 조건을 둔다. 집계가 어긋나도 음수로 내려가지 않게 막는 안전장치다.
     */
    @Modifying
    @Query("update Stock s set s.likeCount = s.likeCount + 1 where s.stockCode = :stockCode")
    int increaseLikeCount(@Param("stockCode") String stockCode);

    @Modifying
    @Query("update Stock s set s.likeCount = s.likeCount - 1 "
            + "where s.stockCode = :stockCode and s.likeCount > 0")
    int decreaseLikeCount(@Param("stockCode") String stockCode);

    /** 관심종목 테이블을 기준으로 관심등록수를 다시 세어 채운다. 집계가 어긋났을 때 복구용. */
    @Modifying
    @Query("update Stock s set s.likeCount = "
            + "(select count(f) from FavoriteStock f where f.stockCode = s.stockCode)")
    int recalculateLikeCounts();
}
