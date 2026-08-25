package com.example.demo.domain.stock.service;

import com.example.demo.api.stock.dto.StockSyncResultDto;
import com.example.demo.domain.stock.entity.StockPriceSnapshot;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface StockCommandService {
    /**
     * @param sourceStocks    현재 구성종목 코드→이름
     * @param unresolvedCodes 구성종목이지만 이번에 데이터를 못 받은 코드.
     *                        편출인지 일시적 실패인지 알 수 없으므로 활성 상태를 건드리지 않는다.
     */
    StockSyncResultDto syncStocks(Map<String, String> sourceStocks, Set<String> unresolvedCodes);

    /** 여러 종목의 시세를 한 번에 갱신하고 실제 반영된 건수를 돌려준다. */
    int updateStockPrices(List<StockPriceSnapshot> snapshots, LocalDate tradeDate);
}
