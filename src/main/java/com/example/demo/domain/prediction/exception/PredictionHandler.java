package com.example.demo.domain.prediction.exception;

import com.example.demo.common.exception.BaseErrorCode;
import com.example.demo.common.exception.GeneralException;

public class PredictionHandler extends GeneralException {

    public static final GeneralException NOT_FOUND
            = new PredictionHandler(PredictionErrorStatus.PREDICTION_NOT_FOUND);

    public static final GeneralException ALREADY_GRADED
            = new PredictionHandler(PredictionErrorStatus.PREDICTION_ALREADY_GRADED);

    public static final GeneralException NOT_MATURED
            = new PredictionHandler(PredictionErrorStatus.PREDICTION_NOT_MATURED);

    public static final GeneralException FORBIDDEN
            = new PredictionHandler(PredictionErrorStatus.PREDICTION_FORBIDDEN);

    public static final GeneralException INVALID_PRICE
            = new PredictionHandler(PredictionErrorStatus.INVALID_PRICE);

    public PredictionHandler(BaseErrorCode errorCode) {
        super(errorCode);
    }
}
