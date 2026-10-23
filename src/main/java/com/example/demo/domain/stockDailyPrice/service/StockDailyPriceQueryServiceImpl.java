package com.example.demo.domain.stockDailyPrice.service;

import com.example.demo.domain.stockDailyPrice.entity.DailyPriceSyncStatus;
import com.example.demo.domain.stockDailyPrice.entity.StockDailyPrice;
import com.example.demo.domain.stockDailyPrice.entity.StockDailyPriceSyncLog;
import com.example.demo.domain.stockDailyPrice.repository.StockDailyPriceRepository;
import com.example.demo.domain.stockDailyPrice.repository.StockDailyPriceSyncLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class StockDailyPriceQueryServiceImpl implements StockDailyPriceQueryService {

    private static final List<DailyPriceSyncStatus> FINISHED =
            Arrays.stream(DailyPriceSyncStatus.values()).filter(DailyPriceSyncStatus::isFinished).toList();
    private static final List<DailyPriceSyncStatus> RETRYABLE =
            Arrays.stream(DailyPriceSyncStatus.values()).filter(s -> !s.isFinished()).toList();

    private final StockDailyPriceRepository stockDailyPriceRepository;
    private final StockDailyPriceSyncLogRepository stockDailyPriceSyncLogRepository;

    @Override
    public List<StockDailyPrice> getDailyPrices(String stockCode, LocalDate from, LocalDate to) {
        return stockDailyPriceRepository.findAllByStockCodeAndTradeDateBetweenOrderByTradeDateAsc(stockCode, from, to);
    }

    @Override
    public Optional<LocalDate> getLatestDailyPriceDate(String stockCode) {
        return stockDailyPriceRepository.findLatestTradeDate(stockCode);
    }

    @Override
    public Optional<LocalDate> getLatestDailyPriceDateOverall() {
        return stockDailyPriceRepository.findLatestTradeDateOverall();
    }

    @Override
    public Optional<LocalDate> getLatestFinishedSyncDate() {
        return stockDailyPriceSyncLogRepository.findLatestTradeDateByStatusIn(FINISHED);
    }

    @Override
    public Optional<LocalDate> getEarliestSyncDate() {
        return stockDailyPriceSyncLogRepository.findEarliestTradeDate();
    }

    @Override
    public List<StockDailyPriceSyncLog> getRetryableSyncLogs(int maxAttempt) {
        return stockDailyPriceSyncLogRepository.findAllByStatusInAndAttemptLessThanOrderByTradeDateAsc(RETRYABLE, maxAttempt);
    }

    @Override
    public Map<LocalDate, StockDailyPriceSyncLog> getSyncLogs(LocalDate from, LocalDate to) {
        return stockDailyPriceSyncLogRepository.findAllByTradeDateBetween(from, to).stream()
                .collect(Collectors.toMap(StockDailyPriceSyncLog::getTradeDate, Function.identity()));
    }
}
