package com.example.demo.domain.stockDailyPrice.service;

import com.example.demo.domain.stockDailyPrice.entity.DailyPriceRow;
import com.example.demo.domain.stockDailyPrice.entity.DailyPriceSyncResult;
import com.example.demo.domain.stockDailyPrice.entity.StockDailyPrice;
import com.example.demo.domain.stockDailyPrice.entity.StockDailyPriceDeleteResult;
import com.example.demo.domain.stockDailyPrice.entity.StockDailyPriceJobLock;
import com.example.demo.domain.stockDailyPrice.entity.StockDailyPriceSyncLog;
import com.example.demo.domain.stockDailyPrice.repository.StockDailyPriceJobLockRepository;
import com.example.demo.domain.stockDailyPrice.repository.StockDailyPriceRepository;
import com.example.demo.domain.stockDailyPrice.repository.StockDailyPriceSyncLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class StockDailyPriceCommandServiceImpl implements StockDailyPriceCommandService {

    private final StockDailyPriceRepository stockDailyPriceRepository;
    private final StockDailyPriceSyncLogRepository stockDailyPriceSyncLogRepository;
    private final StockDailyPriceJobLockRepository stockDailyPriceJobLockRepository;
    private final JdbcTemplate jdbcTemplate;

    /**
     * 신규 일봉 insert. JPA saveAll은 ID 생성 방식(IDENTITY) 때문에 한 건씩 나가서, 3년치 백필(약 15만 행)이 오래 걸린다.
     * 신규는 JDBC 배치로 넣고, 갱신은 지금처럼 변경 감지에 맡긴다. 감사 컬럼(created/last_modified)은 직접 채운다.
     * MySQL에서 실제로 한 문장으로 묶이려면 접속 URL에 rewriteBatchedStatements=true가 있어야 한다.
     */
    private static final String INSERT_SQL = "insert into stock_daily_price "
            + "(stock_code, trade_date, open_price, high_price, low_price, close_price, created_date, last_modified_date) "
            + "values (?, ?, ?, ?, ?, ?, ?, ?)";

    /**
     * 일봉 upsert. 대상 행을 한 번에 읽어 갱신과 신규를 메모리에서 가른다.
     * 행마다 존재 여부를 조회하면 백필 한 묶음(20종목 × 3년)에 만 단위 쿼리가 나간다.
     * 일일 적재는 같은 날짜의 전 종목이라 기간이 하루다.
     */
    @Override
    public int saveDailyPrices(List<DailyPriceRow> rows) {
        // 같은 (종목, 거래일)이 한 요청에 두 번 오면 둘 다 신규로 들어가 유니크 제약에 걸린다. 뒤엣것을 남긴다.
        List<DailyPriceRow> valid = List.copyOf(rows.stream()
                .filter(DailyPriceRow::isValid)
                .collect(Collectors.toMap(r -> dailyKey(r.stockCode(), r.tradeDate()), r -> r,
                        (first, second) -> second, LinkedHashMap::new))
                .values());
        if (valid.isEmpty()) {
            return 0;
        }

        Set<String> codes = valid.stream().map(DailyPriceRow::stockCode).collect(Collectors.toSet());
        LocalDate from = valid.stream().map(DailyPriceRow::tradeDate).min(LocalDate::compareTo).orElseThrow();
        LocalDate to = valid.stream().map(DailyPriceRow::tradeDate).max(LocalDate::compareTo).orElseThrow();

        Map<String, StockDailyPrice> existing = stockDailyPriceRepository
                .findAllByStockCodeInAndTradeDateBetween(codes, from, to).stream()
                .collect(Collectors.toMap(p -> dailyKey(p.getStockCode(), p.getTradeDate()), p -> p));

        List<DailyPriceRow> toInsert = new ArrayList<>();
        for (DailyPriceRow row : valid) {
            StockDailyPrice found = existing.get(dailyKey(row.stockCode(), row.tradeDate()));
            if (found != null) {
                // 변경 감지로 flush된다.
                found.update(row.openPrice(), row.highPrice(), row.lowPrice(), row.closePrice());
                continue;
            }
            toInsert.add(row);
        }
        insertAll(toInsert);

        if (valid.size() < rows.size()) {
            log.warn("가격이 비었거나 0이거나 중복인 일봉 {}건을 버렸습니다.", rows.size() - valid.size());
        }
        return valid.size();
    }

    /**
     * 유니크 충돌이 나면 DuplicateKeyException(DataIntegrityViolationException의 하위)으로 번역되어 나간다.
     * 호출자의 "한 번 다시 시도" 처리가 그대로 동작한다.
     */
    private void insertAll(List<DailyPriceRow> rows) {
        if (rows.isEmpty()) {
            return;
        }
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        jdbcTemplate.batchUpdate(INSERT_SQL, rows, rows.size(), (ps, row) -> {
            ps.setString(1, row.stockCode());
            ps.setObject(2, row.tradeDate());
            ps.setBigDecimal(3, row.openPrice());
            ps.setBigDecimal(4, row.highPrice());
            ps.setBigDecimal(5, row.lowPrice());
            ps.setBigDecimal(6, row.closePrice());
            ps.setTimestamp(7, now);
            ps.setTimestamp(8, now);
        });
    }

    @Override
    public void recordSyncResult(LocalDate tradeDate, DailyPriceSyncResult result) {
        stockDailyPriceSyncLogRepository.findByTradeDate(tradeDate).ifPresentOrElse(
                existing -> existing.apply(result),
                () -> stockDailyPriceSyncLogRepository.save(StockDailyPriceSyncLog.first(tradeDate, result)));
    }

    @Override
    public StockDailyPriceDeleteResult deleteDailyPrices(LocalDate from, LocalDate to, String stockCode) {
        if (stockCode != null) {
            return new StockDailyPriceDeleteResult(
                    stockDailyPriceRepository.deleteAllByStockCodeAndTradeDateBetween(stockCode, from, to), 0);
        }
        int rows = stockDailyPriceRepository.deleteAllByTradeDateBetween(from, to);
        int logs = stockDailyPriceSyncLogRepository.deleteAllByTradeDateBetween(from, to);
        return new StockDailyPriceDeleteResult(rows, logs);
    }

    @Override
    public void createJobLockIfAbsent(String lockName) {
        if (!stockDailyPriceJobLockRepository.existsById(lockName)) {
            stockDailyPriceJobLockRepository.saveAndFlush(StockDailyPriceJobLock.empty(lockName));
        }
    }

    @Override
    public boolean tryAcquireJobLock(String lockName, String owner, LocalDateTime now, LocalDateTime until) {
        return stockDailyPriceJobLockRepository.tryLock(lockName, owner, now, until) == 1;
    }

    @Override
    public boolean extendJobLock(String lockName, String owner, LocalDateTime until) {
        return stockDailyPriceJobLockRepository.extend(lockName, owner, until) == 1;
    }

    @Override
    public void releaseJobLock(String lockName, String owner) {
        stockDailyPriceJobLockRepository.unlock(lockName, owner);
    }

    private static String dailyKey(String stockCode, LocalDate tradeDate) {
        return stockCode + ":" + tradeDate;
    }
}
