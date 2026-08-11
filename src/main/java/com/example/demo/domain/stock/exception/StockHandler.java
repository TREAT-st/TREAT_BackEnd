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

    public static StockHandler s3FileIoError() {
        return new StockHandler(StockErrorStatus.S3_FILE_IO_ERROR);
    }

    public static StockHandler invalidFile() {
        return new StockHandler(StockErrorStatus.INVALID_FILE);
    }

    public static StockHandler fileSizeIsToLarge() {
        return new StockHandler(StockErrorStatus.FILE_TOO_LARGE);
    }

    public static StockHandler emptyStock() {
        return new StockHandler(StockErrorStatus.STOCK_IS_EMPTY);
    }
}
