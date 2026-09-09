package com.example.demo.domain.prediction.exception;

import com.example.demo.common.exception.BaseErrorCode;
import com.example.demo.common.exception.GeneralException;

public class PredictionHandler extends GeneralException {
    public static final GeneralException NOT_FOUND = new PredictionHandler(PredictionErrorStatus.PREDICTION_NOT_FOUND);
    public static final GeneralException ALREADY_GRADED = new PredictionHandler(PredictionErrorStatus.PREDICTION_ALREADY_GRADED);
    public static final GeneralException NOT_MATURED = new PredictionHandler(PredictionErrorStatus.PREDICTION_NOT_MATURED);
    public static final GeneralException STOCK_PRICE_UNAVAILABLE = new PredictionHandler(PredictionErrorStatus.STOCK_PRICE_UNAVAILABLE);

    public PredictionHandler(BaseErrorCode baseErrorCode) {
        super(baseErrorCode);
    }
}