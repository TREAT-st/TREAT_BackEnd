package com.example.demo.domain.favoriteStock.exception;

import com.example.demo.common.exception.BaseErrorCode;
import com.example.demo.common.exception.GeneralException;

public class FavoriteStockHandler extends GeneralException {
    public static final GeneralException NOT_FOUND
            = new FavoriteStockHandler(FavoriteStockErrorStatus.FAVORITE_STOCK_NOT_FOUND);

    public static final GeneralException ALREADY_EXISTS
            = new FavoriteStockHandler(FavoriteStockErrorStatus.FAVORITE_STOCK_ALREADY_EXISTS);

    public FavoriteStockHandler(BaseErrorCode baseErrorCode) {
        super(baseErrorCode);
    }
}
