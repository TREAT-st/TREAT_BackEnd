package com.example.demo.infra.S3.exception;

import com.example.demo.common.exception.BaseErrorCode;
import com.example.demo.common.exception.GeneralException;

public class S3Handler extends GeneralException {

    public static final GeneralException FILE_NOT_FOUND = new S3Handler(S3ErrorStatus.S3_FILE_NOT_FOUND);
    public static final GeneralException ACCESS_DENIED = new S3Handler(S3ErrorStatus.S3_ACCESS_DENIED);
    public static final GeneralException INTERNAL_ERROR = new S3Handler(S3ErrorStatus.S3_INTERNAL_ERROR);

    public S3Handler(BaseErrorCode baseErrorCode) {
        super(baseErrorCode);
    }
}