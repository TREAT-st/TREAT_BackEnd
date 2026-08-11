package com.example.demo.api.kis.exception;

import com.example.demo.common.exception.BaseErrorCode;
import com.example.demo.common.exception.GeneralException;

public class KisHandler extends GeneralException {

    public KisHandler(BaseErrorCode baseErrorCode) {
        super(baseErrorCode);
    }

    public static KisHandler kisApiError() {
        return new KisHandler(KisErrorStatus.KIS_API_ERROR);
    }

    public static KisHandler kisResponseEmpty() {
        return new KisHandler(KisErrorStatus.KIS_RESPONSE_EMPTY);
    }
}
