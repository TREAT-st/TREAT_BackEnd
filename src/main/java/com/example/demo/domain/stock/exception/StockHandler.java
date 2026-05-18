package com.example.demo.domain.stock.exception;

import com.example.demo.common.exception.BaseErrorCode;
import com.example.demo.common.exception.GeneralException;

public class StockHandler extends GeneralException {
    public static final GeneralException NOT_FOUND
            = new StockHandler(StockErrorStatus.STOCK_NOT_FOUND);

    public static final GeneralException STOCK_ALREADY_EXISTS
            = new StockHandler(StockErrorStatus.STOCK_ALREADY_EXISTS);

    public static final GeneralException STOCK_PRICE_NOT_AVAILABLE
            = new StockHandler(StockErrorStatus.STOCK_PRICE_NOT_AVAILABLE);

    public static final GeneralException STOCK_PRICE_RESPONSE_EMPTY
            = new StockHandler(StockErrorStatus.STOCK_PRICE_RESPONSE_EMPTY);

    public StockHandler(BaseErrorCode baseErrorCode) {
        super(baseErrorCode);
    }
}
