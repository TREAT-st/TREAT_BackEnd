package com.example.demo.domain.stock.service;

import com.example.demo.domain.stock.entity.Kospi200SyncCommand;
import com.example.demo.domain.stock.entity.StockPriceSnapshot;
import com.example.demo.domain.stock.entity.StockPriceUpdateResult;
import com.example.demo.domain.stock.entity.StockSyncResult;
import com.example.demo.domain.stock.entity.StockSyncOutcome;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface StockCommandService {
    /**
     * @param sourceStocks    현재 구성종목 코드→이름
     * @param errorCodes Lambda가 데이터를 못 받았다고 보고한 코드 전체.
     *                   이 중 DB에는 있는데 이번 목록에 없는 종목만 편출 판정을 보류한다.
     *                   어떤 종목이 그에 해당하는지는 DB와 비교해야 알 수 있으므로 여기서 가른다.
     */
    StockSyncResult syncStocks(Map<String, String> sourceStocks, Set<String> errorCodes);

    /** 여러 종목의 시세를 한 번에 갱신하고, 반영 건수와 반영하지 못한 종목 코드를 돌려준다. */
    StockPriceUpdateResult updateStockPrices(List<StockPriceSnapshot> snapshots, LocalDate tradeDate);

    /**
     * 목록 동기화와 시세 갱신을 한 트랜잭션으로 묶어 실행한다.
     * KRX Lambda 호출은 수 분이 걸릴 수 있어 이 메서드 밖에서 끝내고 결과만 넘긴다.
     * 트랜잭션이 DB 쓰기 구간만 감싸게 하면서, 두 작업의 원자성은 그대로 유지하기 위한 진입점이다.
     */
    StockSyncOutcome syncStocksAndPrices(Kospi200SyncCommand command);
}
