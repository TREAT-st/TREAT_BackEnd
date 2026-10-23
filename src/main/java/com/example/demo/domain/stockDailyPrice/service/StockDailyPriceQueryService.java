package com.example.demo.domain.stockDailyPrice.service;

import com.example.demo.domain.stockDailyPrice.entity.StockDailyPrice;
import com.example.demo.domain.stockDailyPrice.entity.StockDailyPriceSyncLog;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface StockDailyPriceQueryService {

    /** 종목의 일봉. 기간 양 끝 포함, 거래일 오름차순. */
    List<StockDailyPrice> getDailyPrices(String stockCode, LocalDate from, LocalDate to);

    /** 그 종목에 쌓인 마지막 일봉의 거래일. 아직 없으면 비어 있다. */
    Optional<LocalDate> getLatestDailyPriceDate(String stockCode);

    /** 일봉 테이블 전체의 마지막 거래일. 일일 적재 이력이 없을 때의 시작점으로만 쓴다. */
    Optional<LocalDate> getLatestDailyPriceDateOverall();

    /** 끝난 상태(SUCCESS·HOLIDAY)인 마지막 거래일. */
    Optional<LocalDate> getLatestFinishedSyncDate();

    /** 첫 적재 이력의 거래일. 이력이 없으면 비어 있다. */
    Optional<LocalDate> getEarliestSyncDate();

    /** 시도 횟수가 maxAttempt 미만인 PARTIAL·FAILED 이력. 거래일 오름차순. */
    List<StockDailyPriceSyncLog> getRetryableSyncLogs(int maxAttempt);

    /** 기간의 적재 이력. 거래일 → 이력. */
    Map<LocalDate, StockDailyPriceSyncLog> getSyncLogs(LocalDate from, LocalDate to);
}
