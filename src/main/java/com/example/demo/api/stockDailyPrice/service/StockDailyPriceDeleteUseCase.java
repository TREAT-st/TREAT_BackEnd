package com.example.demo.api.stockDailyPrice.service;

import com.example.demo.common.annotation.UseCase;
import com.example.demo.domain.stock.service.StockQueryService;
import com.example.demo.domain.stockDailyPrice.entity.StockDailyPriceDeleteResult;
import com.example.demo.domain.stockDailyPrice.exception.StockDailyPriceHandler;
import com.example.demo.domain.stockDailyPrice.service.StockDailyPriceCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;

/**
 * 일봉 기간 삭제. 동기로 처리하고 지운 행 수를 돌려준다.
 *
 * 다시 채우기:
 *   - 전 종목을 최근 30일 안에서 지우면 그 기간의 이력도 지워지므로, 다음 일일 적재가 빈 날로 보고 다시 받는다
 *   - 그보다 오래된 구간이나 한 종목만 지우면 자동으로 다시 채워지지 않는다. /backfill을 같은 기간
 *     (한 종목이면 stockCodes까지)으로 호출한다. 오래된 구간을 정리 목적으로 지운 경우 다시 받지 않는 것이 의도다
 */
@Slf4j
@UseCase
@RequiredArgsConstructor
public class StockDailyPriceDeleteUseCase {

    private final StockQueryService stockQueryService;
    private final StockDailyPriceCommandService stockDailyPriceCommandService;
    private final DailyPriceJobGuard dailyPriceJobGuard;

    /**
     * @param stockCode 없으면 전 종목. 전 종목 삭제는 그 기간의 일일 적재 이력도 함께 지운다
     *                  (이력은 성공인데 데이터가 없는 상태를 남기지 않는다). 있으면 그 종목만 지우고 이력은 그대로 둔다.
     *                  stock에 없는 종목이면 404(4250)
     *
     * 백필·일일 적재와 같은 잠금을 잡고 지운다. 적재 중에 지우면 지운 데이터가 진행 중인 적재로 다시 들어가거나,
     * 일봉은 없는데 성공 이력이 다시 기록될 수 있다. 적재가 돌고 있으면 409(4258)로 거절한다.
     */
    public StockDailyPriceDeleteResult delete(LocalDate from, LocalDate to, String stockCode) {
        if (from == null || to == null || from.isAfter(to)) {
            throw StockDailyPriceHandler.invalidRange();
        }
        if (stockCode != null) {
            stockQueryService.getStockByCode(stockCode);
        }

        String lockToken = dailyPriceJobGuard.tryAcquire().orElseThrow(StockDailyPriceHandler::jobRunning);
        try {
            StockDailyPriceDeleteResult result = stockDailyPriceCommandService.deleteDailyPrices(from, to, stockCode);
            log.info("일봉 삭제. from={} to={} stockCode={} 삭제행={} 삭제이력={}",
                    from, to, stockCode == null ? "전체" : stockCode, result.deletedRows(), result.deletedSyncLogs());
            return result;
        } finally {
            dailyPriceJobGuard.release(lockToken);
        }
    }
}
