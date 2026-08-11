package com.example.demo.api.krx.exception;

import com.example.demo.common.exception.BaseErrorCode;
import com.example.demo.common.exception.GeneralException;

public class KrxHandler extends GeneralException {

    public KrxHandler(BaseErrorCode baseErrorCode) {
        super(baseErrorCode);
    }

    public static KrxHandler krxLambdaInvokeError() {
        return new KrxHandler(KrxErrorStatus.KRX_LAMBDA_INVOKE_ERROR);
    }

    public static KrxHandler krxLambdaResponseEmpty() {
        return new KrxHandler(KrxErrorStatus.KRX_LAMBDA_RESPONSE_EMPTY);
    }

    public static KrxHandler krxLambdaExecutionError() {
        return new KrxHandler(KrxErrorStatus.KRX_LAMBDA_EXECUTION_ERROR);
    }

    public static KrxHandler krxLambdaResponseParseError() {
        return new KrxHandler(KrxErrorStatus.KRX_LAMBDA_RESPONSE_PARSE_ERROR);
    }
}
