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

    public static StockHandler stockAlreadyExists() {
        return new StockHandler(StockErrorStatus.STOCK_ALREADY_EXISTS);
    }

    public static StockHandler stockPriceNotAvailable() {
        return new StockHandler(StockErrorStatus.STOCK_PRICE_NOT_AVAILABLE);
    }

    public static StockHandler stockPriceResponseEmpty() {
        return new StockHandler(StockErrorStatus.STOCK_PRICE_RESPONSE_EMPTY);
    }

    public static StockHandler kisApiError() {
        return new StockHandler(StockErrorStatus.KIS_API_ERROR);
    }

    public static StockHandler s3FileIoError() {
        return new StockHandler(StockErrorStatus.S3_FILE_IO_ERROR);
    }

    public static StockHandler invalidFile() {
        return new StockHandler(StockErrorStatus.INVALID_FILE);
    }
}
