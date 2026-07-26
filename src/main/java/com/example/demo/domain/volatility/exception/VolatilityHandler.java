package com.example.demo.domain.volatility.exception;

import com.example.demo.common.exception.BaseErrorCode;
import com.example.demo.common.exception.GeneralException;

public class VolatilityHandler extends GeneralException {
    public static final GeneralException NOT_FOUND
            = new VolatilityHandler(VolatilityErrorStatus.VOLATILITY_NOT_FOUND);

    public VolatilityHandler(BaseErrorCode baseErrorCode) {
        super(baseErrorCode);
    }
}
