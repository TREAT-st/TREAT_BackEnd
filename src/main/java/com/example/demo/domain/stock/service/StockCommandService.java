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
     *
     * 진입부에서 거래일을 검증하며, 어떤 쓰기보다 먼저 막으므로 거절되면 DB는 그대로 남는다.
     *
     * @param force 이미 반영된 거래일을 다시 반영할지 여부.
     *              시세를 못 받은 종목을 같은 거래일로 재시도할 때만 쓴다.
     *              과거 거래일과 오늘 이후 날짜는 force와 무관하게 막힌다.
     */
    StockSyncOutcome syncStocksAndPrices(Kospi200SyncCommand command, boolean force);

    /**
     * 관심등록수를 1 올린다. 관심종목 등록과 같은 트랜잭션에서 호출해야 집계가 어긋나지 않는다.
     * 해당 종목이 stock에 없으면 아무것도 하지 않는다.
     */
    void increaseLikeCount(String stockCode);

    /** 관심등록수를 1 내린다. 0 아래로는 내려가지 않는다. */
    void decreaseLikeCount(String stockCode);
}
