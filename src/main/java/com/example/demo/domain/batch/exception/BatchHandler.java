package com.example.demo.domain.batch.exception;

import com.example.demo.common.exception.BaseErrorCode;
import com.example.demo.common.exception.GeneralException;

public class BatchHandler extends GeneralException {

    public BatchHandler(BaseErrorCode baseErrorCode) {
        super(baseErrorCode);
    }

    public static BatchHandler executionNotFound() {
        return new BatchHandler(BatchErrorStatus.BATCH_EXECUTION_NOT_FOUND);
    }

    public static BatchHandler triggerUnauthorized() {
        return new BatchHandler(BatchErrorStatus.BATCH_TRIGGER_UNAUTHORIZED);
    }

}
