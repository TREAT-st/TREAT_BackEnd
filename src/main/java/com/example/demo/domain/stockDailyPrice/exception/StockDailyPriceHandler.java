package com.example.demo.domain.stockDailyPrice.exception;

import com.example.demo.common.exception.BaseErrorCode;
import com.example.demo.common.exception.GeneralException;

public class StockDailyPriceHandler extends GeneralException {

    public StockDailyPriceHandler(BaseErrorCode baseErrorCode) {
        super(baseErrorCode);
    }

    public static StockDailyPriceHandler invalidRange() {
        return new StockDailyPriceHandler(StockDailyPriceErrorStatus.DAILY_PRICE_INVALID_RANGE);
    }

    public static StockDailyPriceHandler priceNotSyncedYet() {
        return new StockDailyPriceHandler(StockDailyPriceErrorStatus.DAILY_PRICE_NOT_SYNCED_YET);
    }

    public static StockDailyPriceHandler jobRunning() {
        return new StockDailyPriceHandler(StockDailyPriceErrorStatus.DAILY_PRICE_JOB_RUNNING);
    }

    public static StockDailyPriceHandler unknownStockCodes() {
        return new StockDailyPriceHandler(StockDailyPriceErrorStatus.DAILY_PRICE_UNKNOWN_STOCK_CODES);
    }
}
