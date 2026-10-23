package com.example.demo.api.stockDailyPrice.mapper;

import com.example.demo.api.stockDailyPrice.dto.StockDailyPriceResponseDto.BackfillTriggerResponse;
import com.example.demo.api.stockDailyPrice.dto.StockDailyPriceResponseDto.ChartCandleResponse;
import com.example.demo.api.stockDailyPrice.dto.StockDailyPriceResponseDto.DailyTriggerResponse;
import com.example.demo.api.stockDailyPrice.dto.StockDailyPriceResponseDto.DeleteResponse;
import com.example.demo.api.stockDailyPrice.dto.StockDailyPriceResponseDto.StockChartResponse;
import com.example.demo.api.stockDailyPrice.service.StockDailyPriceBackfillUseCase.BackfillPlan;
import com.example.demo.api.stockDailyPrice.service.StockDailyPriceSyncUseCase.DailyPlan;
import com.example.demo.domain.stock.entity.Stock;
import com.example.demo.domain.stockDailyPrice.entity.ChartRange;
import com.example.demo.domain.stockDailyPrice.entity.StockDailyPrice;
import com.example.demo.domain.stockDailyPrice.entity.StockDailyPriceDeleteResult;

import java.time.LocalDate;
import java.util.List;

public class StockDailyPriceConverter {

    /** 응답의 봉 단위. 모든 기간이 일봉이라 고정이다. */
    private static final String INTERVAL_DAY = "day";

    public static StockChartResponse toStockChartResponse(Stock stock, ChartRange range, List<StockDailyPrice> dailyPrices) {
        return StockChartResponse.builder()
                .stockCode(stock.getStockCode())
                .stockName(stock.getStockName())
                .range(range.getCode())
                .interval(INTERVAL_DAY)
                .data(dailyPrices.stream()
                        .map(p -> ChartCandleResponse.builder()
                                .tradeDate(p.getTradeDate())
                                .openPrice(p.getOpenPrice())
                                .highPrice(p.getHighPrice())
                                .lowPrice(p.getLowPrice())
                                .closePrice(p.getClosePrice())
                                .build())
                        .toList())
                .build();
    }

    public static BackfillTriggerResponse toBackfillTriggerResponse(boolean accepted, String message, BackfillPlan plan) {
        return BackfillTriggerResponse.builder()
                .accepted(accepted)
                .message(message)
                .from(plan.from())
                .to(plan.to())
                .targetStockCount(plan.stockCodes().size())
                .build();
    }

    public static DailyTriggerResponse toDailyTriggerResponse(boolean accepted, String message, DailyPlan plan) {
        return DailyTriggerResponse.builder()
                .accepted(accepted)
                .message(message)
                .latestTradeDate(plan.latestTradeDate())
                .tradeDates(plan.tradeDates())
                .build();
    }

    public static DeleteResponse toDeleteResponse(LocalDate from, LocalDate to, String stockCode,
                                                  StockDailyPriceDeleteResult result, String message) {
        return DeleteResponse.builder()
                .from(from)
                .to(to)
                .stockCode(stockCode)
                .deletedRows(result.deletedRows())
                .deletedSyncLogs(result.deletedSyncLogs())
                .message(message)
                .build();
    }
}
