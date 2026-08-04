package com.example.demo.domain.volatility.exception;

import com.example.demo.common.exception.BaseErrorCode;
import com.example.demo.common.exception.GeneralException;

public class VolatilityHandler extends GeneralException {
    public static final GeneralException NOT_FOUND
            = new VolatilityHandler(VolatilityErrorStatus.VOLATILITY_NOT_FOUND);

    public VolatilityHandler(BaseErrorCode baseErrorCode) {
        super(baseErrorCode);
    }

    public static VolatilityHandler volatilityNotFound() {
        return new VolatilityHandler(VolatilityErrorStatus.VOLATILITY_NOT_FOUND);
    }

    public static VolatilityHandler krxLambdaInvokeError() {
        return new VolatilityHandler(VolatilityErrorStatus.KRX_LAMBDA_INVOKE_ERROR);
    }

    public static VolatilityHandler krxLambdaResponseEmpty() {
        return new VolatilityHandler(VolatilityErrorStatus.KRX_LAMBDA_RESPONSE_EMPTY);
    }
}
