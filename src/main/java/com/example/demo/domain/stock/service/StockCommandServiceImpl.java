package com.example.demo.domain.stock.service;

import com.example.demo.api.stock.dto.StockSyncResultDto;
import com.example.demo.domain.stock.entity.Stock;
import com.example.demo.domain.stock.entity.StockPriceSnapshot;
import com.example.demo.domain.stock.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class StockCommandServiceImpl implements StockCommandService {

    private final StockRepository stockRepository;

    /**
     * 코스피200에 편입된(활성) 종목의 시세만 갱신한다.
     * 종목별로 findByStockCode를 반복하면 200번 조회가 나가므로 한 번에 읽어 메모리에서 갱신하며,
     * 변경 감지로 flush된다. DB에 없거나 편출된 종목은 건너뛴다.
     */
    @Override
    public int updateStockPrices(List<StockPriceSnapshot> snapshots, LocalDate tradeDate) {
        if (snapshots.isEmpty()) {
            return 0;
        }

        Map<String, StockPriceSnapshot> byCode = snapshots.stream()
                .collect(Collectors.toMap(StockPriceSnapshot::stockCode, snapshot -> snapshot));

        List<Stock> stocks = stockRepository.findAllByStockCodeInAndIsActiveTrue(byCode.keySet());

        for (Stock stock : stocks) {
            StockPriceSnapshot snapshot = byCode.get(stock.getStockCode());
            stock.updatePrice(snapshot.openPrice(), snapshot.closePrice(), tradeDate);
        }

        int skipped = byCode.size() - stocks.size();
        if (skipped > 0) {
            log.warn("시세를 반영하지 못한 종목 {}건. DB에 없거나 비활성 종목입니다.", skipped);
        }

        return stocks.size();
    }

    @Override
    public StockSyncResultDto syncStocks(Map<String, String> sourceStocks, Set<String> unresolvedCodes) {
        List<Stock> dbStocks = stockRepository.findAll();
        Map<String, Stock> byCode = dbStocks.stream()
                .collect(Collectors.toMap(Stock::getStockCode, stock -> stock));

        List<Stock> toInsert = new ArrayList<>();
        int updatedCount = 0;
        int reactivatedCount = 0;

        for (Map.Entry<String, String> entry : sourceStocks.entrySet()) {
            String stockCode = entry.getKey();
            String stockName = entry.getValue();
            Stock stock = byCode.get(stockCode);

            if (stock == null) {
                toInsert.add(Stock.builder()
                        .stockCode(stockCode)
                        .stockName(stockName)
                        .build());
                continue;
            }

            if (!stockName.equals(stock.getStockName())) {
                stock.updateName(stockName);
                updatedCount++;
            }
            if (!Boolean.TRUE.equals(stock.getIsActive())) {
                stock.activate();
                reactivatedCount++;
                log.info("종목 재편입. stockCode={}, stockName={}", stockCode, stockName);
            }
        }

        int deactivatedCount = 0;
        for (Stock stock : dbStocks) {
            String stockCode = stock.getStockCode();

            if (sourceStocks.containsKey(stockCode) || !Boolean.TRUE.equals(stock.getIsActive())) {
                continue;
            }

            // 구성종목이지만 시세를 못 받은 종목이다. 거래정지처럼 일시적인 경우가 많아
            // 편출로 단정하면 안 된다. 이번 회차는 상태를 그대로 둔다.
            if (unresolvedCodes.contains(stockCode)) {
                log.info("시세 미수신으로 편출 판정 보류. stockCode={}, stockName={}",
                        stockCode, stock.getStockName());
                continue;
            }

            stock.deactivate();
            deactivatedCount++;
            log.info("종목 편출. stockCode={}, stockName={}", stockCode, stock.getStockName());
        }

        if (!toInsert.isEmpty()) {
            stockRepository.saveAll(toInsert);
        }

        return new StockSyncResultDto(toInsert.size(), updatedCount, deactivatedCount, reactivatedCount);
    }
}
