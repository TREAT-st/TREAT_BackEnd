package com.example.demo.domain.stock.service;

import com.example.demo.domain.stock.entity.Kospi200SyncCommand;
import com.example.demo.domain.stock.entity.Stock;
import com.example.demo.domain.stock.exception.StockHandler;
import com.example.demo.domain.stock.entity.StockSyncResult;
import com.example.demo.domain.stock.entity.StockPriceSnapshot;
import com.example.demo.domain.stock.entity.StockPriceUpdateResult;
import com.example.demo.domain.stock.entity.StockSyncOutcome;
import com.example.demo.domain.stock.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class StockCommandServiceImpl implements StockCommandService {

    /**
     * 한 번에 이만큼 넘게 편출 판정이 나오면 데이터 이상으로 보고 막는다.
     * 코스피200 정기변경은 연 2회, 보통 3~10종목이라 정상 운영에서는 걸리지 않는다.
     * 지수 구성종목 목록이 일부만 조회되면 빠진 종목이 전부 편출로 판정되는데,
     * "목록이 비었을 때"만 막는 기존 가드로는 이 경우를 못 잡는다.
     */
    private static final int DEACTIVATION_LIMIT_COUNT = 20;
    private static final double DEACTIVATION_LIMIT_RATIO = 0.1;

    private final StockRepository stockRepository;

    /**
     * 두 작업을 한 트랜잭션 안에서 순서대로 실행한다.
     * 같은 빈 안에서의 호출이라 프록시를 타지 않지만, 전파 속성이 REQUIRED라
     * 어차피 이 메서드의 트랜잭션에 합류했을 동작과 동일하다.
     */
    @Override
    public StockSyncOutcome syncStocksAndPrices(Kospi200SyncCommand command) {
        StockSyncResult syncResult = syncStocks(command.incomingStocks(), command.errorCodes());
        StockPriceUpdateResult priceResult = updateStockPrices(command.priceSnapshots(), command.tradeDate());

        log.info("코스피200 동기화 완료. tradeDate={} 구성종목={} 신규={} 이름변경={} 이탈={} 재편입={} "
                        + "시세반영={} 시세미수신={} 반영실패={} 판정보류={}",
                command.tradeDate(), command.incomingStocks().size(),
                syncResult.addedCount(), syncResult.updatedCount(),
                syncResult.deactivatedCount(), syncResult.reactivatedCount(),
                priceResult.updatedCount(), command.priceUnavailableStockCodes().size(),
                priceResult.skippedStockCodes().size(), syncResult.unresolvedStockCodes().size());

        return new StockSyncOutcome(syncResult, priceResult.updatedCount(), priceResult.skippedStockCodes());
    }

    /**
     * 코스피200에 편입된(활성) 종목의 시세만 갱신한다.
     * 종목별로 findByStockCode를 반복하면 200번 조회가 나가므로 한 번에 읽어 메모리에서 갱신하며,
     * 변경 감지로 flush된다. DB에 없거나 편출된 종목은 건너뛴다.
     */
    @Override
    public StockPriceUpdateResult updateStockPrices(List<StockPriceSnapshot> snapshots, LocalDate tradeDate) {
        if (snapshots.isEmpty()) {
            return new StockPriceUpdateResult(0, List.of());
        }

        Map<String, StockPriceSnapshot> byCode = snapshots.stream()
                .collect(Collectors.toMap(StockPriceSnapshot::stockCode, snapshot -> snapshot));

        List<Stock> stocks = stockRepository.findAllByStockCodeInAndIsActiveTrue(byCode.keySet());

        Set<String> appliedCodes = new HashSet<>();
        for (Stock stock : stocks) {
            StockPriceSnapshot snapshot = byCode.get(stock.getStockCode());
            stock.updatePrice(snapshot.openPrice(), snapshot.closePrice(), tradeDate);
            appliedCodes.add(stock.getStockCode());
        }

        // 어떤 종목이 빠졌는지 남긴다. 같은 실행에서 목록 동기화를 끝낸 뒤라
        // 여기서 빠지는 종목이 있으면 단순 누락이 아니라 동기화 정합성 이상 신호다.
        List<String> skippedStockCodes = byCode.keySet().stream()
                .filter(code -> !appliedCodes.contains(code))
                .sorted()
                .toList();

        if (!skippedStockCodes.isEmpty()) {
            log.warn("시세를 반영하지 못한 종목 {}건. DB에 없거나 비활성 종목입니다. codes={}",
                    skippedStockCodes.size(), skippedStockCodes);
        }

        return new StockPriceUpdateResult(appliedCodes.size(), skippedStockCodes);
    }

    /**
     * 편출 규모가 정상 범위인지 확인한다.
     * 지수 구성종목 목록이 일부만 조회되면 빠진 종목이 전부 편출로 판정되므로,
     * 반영 전에 막아 트랜잭션을 되돌린다.
     */
    private void verifyDeactivationScale(Map<String, Stock> stored, List<String> toDeactivate) {
        long activeCount = stored.values().stream()
                .filter(stock -> Boolean.TRUE.equals(stock.getIsActive()))
                .count();

        if (toDeactivate.size() > DEACTIVATION_LIMIT_COUNT
                && toDeactivate.size() > activeCount * DEACTIVATION_LIMIT_RATIO) {
            log.error("편출 판정이 비정상적으로 많아 동기화를 중단합니다. 활성={} 편출대상={} codes={}",
                    activeCount, toDeactivate.size(), toDeactivate);
            throw StockHandler.abnormalDeactivation();
        }
    }

    /**
     * 3~6단계. 응답 목록과 DB 목록을 비교해 세 갈래로 가른 뒤 각각 반영한다.
     * 판정(무엇이 어느 갈래인가)과 반영(무엇을 할 것인가)을 분리해야, 결과만 보고도 왜 그렇게 됐는지 읽힌다.
     */
    @Override
    public StockSyncResult syncStocks(Map<String, String> incomingStocks, Set<String> errorCodes) {
        // 3. 현재 저장된 종목
        Map<String, Stock> stored = stockRepository.findAll().stream()
                .collect(Collectors.toMap(Stock::getStockCode, stock -> stock));

        Set<String> incomingCodes = incomingStocks.keySet();

        // 4~5. 세 갈래로 분류
        //  ① 유지 : 응답에도 있고 DB에도 있는 종목
        //  ② 신규 : 응답에 있고 DB에 없는 종목 (이번에 코스피200 편입)
        //  ③ 이탈 : DB에 활성으로 있는데 응답에 없는 종목 (이번에 코스피200 이탈)
        List<String> retainedCodes = incomingCodes.stream()
                .filter(stored::containsKey)
                .sorted()
                .toList();
        List<String> newCodes = incomingCodes.stream()
                .filter(code -> !stored.containsKey(code))
                .sorted()
                .toList();
        List<String> droppedCodes = stored.keySet().stream()
                .filter(code -> !incomingCodes.contains(code))
                .filter(code -> Boolean.TRUE.equals(stored.get(code).getIsActive()))
                .sorted()
                .toList();

        // 6-①. 유지 종목은 이름 변경과 재활성만 반영한다. 변경 감지로 flush된다.
        int updatedCount = 0;
        int reactivatedCount = 0;
        for (String stockCode : retainedCodes) {
            Stock stock = stored.get(stockCode);
            String stockName = incomingStocks.get(stockCode);

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

        // 6-②. 신규 종목은 한 번에 저장한다.
        List<Stock> toInsert = new ArrayList<>();
        for (String stockCode : newCodes) {
            toInsert.add(Stock.builder()
                    .stockCode(stockCode)
                    .stockName(incomingStocks.get(stockCode))
                    .build());
        }
        if (!toInsert.isEmpty()) {
            stockRepository.saveAll(toInsert);
        }

        // 6-③. 이탈 종목은 삭제하지 않고 비활성 처리한다.
        //      단, Lambda가 데이터를 못 받았다고 보고한 종목은 이탈인지 일시적 실패인지 알 수 없다.
        //      거래정지 종목을 편출로 오판하면 목록에서 사라지므로, 이번 회차는 상태를 그대로 둔다.
        List<String> unresolvedStockCodes = droppedCodes.stream()
                .filter(errorCodes::contains)
                .toList();
        List<String> toDeactivate = droppedCodes.stream()
                .filter(code -> !errorCodes.contains(code))
                .toList();

        verifyDeactivationScale(stored, toDeactivate);

        for (String stockCode : unresolvedStockCodes) {
            log.info("데이터 미수신으로 이탈 판정 보류. stockCode={}, stockName={}",
                    stockCode, stored.get(stockCode).getStockName());
        }
        for (String stockCode : toDeactivate) {
            Stock stock = stored.get(stockCode);
            stock.deactivate();
            log.info("종목 이탈. stockCode={}, stockName={}", stockCode, stock.getStockName());
        }
        int deactivatedCount = toDeactivate.size();

        // 7. 결과
        return new StockSyncResult(
                toInsert.size(), updatedCount, deactivatedCount, reactivatedCount, unresolvedStockCodes);
    }
}
