package com.example.demo.domain.volatility.repository;

import com.example.demo.domain.volatility.entity.Volatility;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface VolatilityRepository extends JpaRepository<Volatility, Long> {
    List<Volatility> findAllByStockCodeOrderByTradeDateDesc(String stockCode);

    Optional<Volatility> findFirstByStockCodeOrderByTradeDateDesc(String stockCode);

    List<Volatility> findAllByTradeDateOrderByIdAsc(LocalDate tradeDate);

    /**
     * 저장 경로 전용. 같은 거래일에 대한 동시 탐지가 서로의 스냅샷을 못 보고
     * 삭제·삽입을 교차시키면 최종 집합이 어느 쪽 결과와도 달라진다.
     * 해당 거래일의 행을 잠가 회차 단위로 직렬화한다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM Volatility v WHERE v.tradeDate = :tradeDate ORDER BY v.id ASC")
    List<Volatility> findAllByTradeDateForUpdate(@Param("tradeDate") LocalDate tradeDate);

    /** (stock_code, trade_date) 유니크 제약이 있으므로 최대 1건이다. */
    Optional<Volatility> findByStockCodeAndTradeDate(String stockCode, LocalDate tradeDate);

    /** 리포트가 아직 도착하지 않은 종목 수. 도착 확인 단계를 닫을지 판단할 때 쓴다. */
    long countByTradeDateAndReportUrlIsNull(LocalDate tradeDate);

    /** 리포트가 아직 도착하지 않은 종목. S3에서 찾아 메울 대상이다. */
    List<Volatility> findAllByTradeDateAndReportUrlIsNull(LocalDate tradeDate);

    /**
     * 비어 있을 때만 채운다. S3 보정 전용이고, 콜백은 기존 updateReportUrl을 그대로 쓴다.
     *
     * 보정은 조회 시점의 판단이라 갱신할 때는 이미 낡았을 수 있다. 그 사이 콜백이 채웠다면
     * 그쪽이 더 최신이므로 덮으면 안 된다. 조건을 DB에 맡겨야 그 틈이 사라진다.
     *
     * @return 갱신된 행 수. 0이면 콜백이 먼저 채운 것이다.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Volatility v SET v.reportUrl = :reportUrl "
            + "WHERE v.stockCode = :stockCode AND v.tradeDate = :tradeDate AND v.reportUrl IS NULL")
    int fillReportUrlIfAbsent(@Param("stockCode") String stockCode,
                              @Param("tradeDate") LocalDate tradeDate,
                              @Param("reportUrl") String reportUrl);

    /** 탐지 기록이 있는 가장 최근 거래일을 찾기 위한 조회. 리포트 생성 대상을 정할 때 쓴다. */
    Optional<Volatility> findFirstByOrderByTradeDateDesc();
}
