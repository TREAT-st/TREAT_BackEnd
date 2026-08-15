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
}
