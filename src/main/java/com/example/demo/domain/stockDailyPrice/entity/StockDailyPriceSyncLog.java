package com.example.demo.domain.stockDailyPrice.entity;

import com.example.demo.domain.model.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * 거래일별 일일 적재 이력. 일일 적재(/daily)가 어느 날짜를 다시 받아야 하는지를 이 표로 정한다.
 *
 * 일봉 테이블의 최신 날짜만 보면 일부 종목 실패를 복구하지 못한다. 199종목이 저장되면 최신 날짜는 이미
 * 그날이 되어, 실패한 한 종목의 그날은 다시 요청되지 않는다. 그래서 날짜마다 결과와 실패 종목을 남긴다.
 *
 * 백필(/backfill)은 이 표를 쓰지 않는다. 백필 결과는 로그로 남고, 실패 종목은 stockCodes로 다시 백필한다.
 */
@Entity
@Table(name = "stock_daily_price_sync_log",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_stock_daily_price_sync_log_trade_date",
                columnNames = "trade_date"))
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class StockDailyPriceSyncLog extends BaseTimeEntity {

    private static final int MESSAGE_MAX_LENGTH = 1000;
    private static final String CODE_DELIMITER = ",";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stock_daily_price_sync_log_id")
    private Long id;

    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DailyPriceSyncStatus status;

    /** 시도 횟수. 상한을 넘은 PARTIAL·FAILED는 자동 재시도에서 빠지고 수동 처리 대상이 된다. */
    @Column(name = "attempt", nullable = false)
    private int attempt;

    @Column(name = "requested_stock_count", nullable = false)
    private int requestedStockCount;

    /** 저장한 행 수(신규+갱신). 일일 적재는 종목당 한 행이라 저장한 종목 수와 같다. */
    @Column(name = "upserted_rows", nullable = false)
    private int upsertedRows;

    /** 거래정지·미상장 등 가격이 없는 것이 정상인 종목 수. 실패가 아니다. */
    @Column(name = "skipped_stock_count", nullable = false)
    private int skippedStockCount;

    /** 다음 시도에서 다시 요청할 종목. 쉼표로 잇는다. 코스피200 규모라 길이가 넉넉하다. */
    @Lob
    @Column(name = "failed_stock_codes")
    private String failedStockCodes;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "finished_at", nullable = false)
    private LocalDateTime finishedAt;

    @Column(name = "message", length = MESSAGE_MAX_LENGTH)
    private String message;

    public static StockDailyPriceSyncLog first(LocalDate tradeDate, DailyPriceSyncResult result) {
        StockDailyPriceSyncLog log = StockDailyPriceSyncLog.builder().tradeDate(tradeDate).attempt(0).build();
        log.apply(result);
        return log;
    }

    /** 같은 거래일을 다시 받은 결과를 덮어쓰고 시도 횟수를 올린다. */
    public void apply(DailyPriceSyncResult result) {
        this.status = result.status();
        this.attempt++;
        this.requestedStockCount = result.requestedStockCount();
        this.upsertedRows = result.upsertedRows();
        this.skippedStockCount = result.skippedStockCount();
        this.failedStockCodes = result.failedStockCodes().isEmpty()
                ? null : String.join(CODE_DELIMITER, result.failedStockCodes());
        this.startedAt = result.startedAt();
        this.finishedAt = result.finishedAt();
        this.message = truncate(result.message());
    }

    public List<String> failedStockCodeList() {
        if (failedStockCodes == null || failedStockCodes.isBlank()) {
            return List.of();
        }
        return Arrays.stream(failedStockCodes.split(CODE_DELIMITER)).map(String::trim).filter(c -> !c.isEmpty()).toList();
    }

    private static String truncate(String message) {
        if (message == null || message.length() <= MESSAGE_MAX_LENGTH) {
            return message;
        }
        return message.substring(0, MESSAGE_MAX_LENGTH);
    }
}
