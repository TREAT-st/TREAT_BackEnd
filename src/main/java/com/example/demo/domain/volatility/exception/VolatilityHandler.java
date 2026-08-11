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

    public static VolatilityHandler volatilityNotDetectedToday() {
        return new VolatilityHandler(VolatilityErrorStatus.VOLATILITY_NOT_DETECTED_TODAY);
    }

    public static VolatilityHandler reportLambdaInvokeError() {
        return new VolatilityHandler(VolatilityErrorStatus.REPORT_LAMBDA_INVOKE_ERROR);
    }

    public static VolatilityHandler reportCallbackUnauthorized() {
        return new VolatilityHandler(VolatilityErrorStatus.REPORT_CALLBACK_UNAUTHORIZED);
    }

    public static VolatilityHandler reportCallbackInvalidRequest() {
        return new VolatilityHandler(VolatilityErrorStatus.REPORT_CALLBACK_INVALID_REQUEST);
    }
}
