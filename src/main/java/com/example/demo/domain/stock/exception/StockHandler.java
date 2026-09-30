package com.example.demo.domain.stock.exception;

import com.example.demo.common.exception.BaseErrorCode;
import com.example.demo.common.exception.GeneralException;

public class StockHandler extends GeneralException {

    public StockHandler(BaseErrorCode baseErrorCode) {
        super(baseErrorCode);
    }

    public static StockHandler notFound() {
        return new StockHandler(StockErrorStatus.STOCK_NOT_FOUND);
    }

    public static StockHandler abnormalDeactivation() {
        return new StockHandler(StockErrorStatus.STOCK_ABNORMAL_DEACTIVATION);
    }

    public static StockHandler tradeDateAlreadySynced() {
        return new StockHandler(StockErrorStatus.STOCK_TRADE_DATE_ALREADY_SYNCED);
    }

    public static StockHandler staleTradeDate() {
        return new StockHandler(StockErrorStatus.STOCK_STALE_TRADE_DATE);
    }

    public static StockHandler invalidTradeDate() {
        return new StockHandler(StockErrorStatus.STOCK_INVALID_TRADE_DATE);
    }
}
