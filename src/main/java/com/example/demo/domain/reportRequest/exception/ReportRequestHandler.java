package com.example.demo.domain.reportRequest.exception;

import com.example.demo.common.exception.BaseErrorCode;
import com.example.demo.common.exception.GeneralException;


public class ReportRequestHandler extends GeneralException {
    public static final GeneralException NOT_FOUND
            = new ReportRequestHandler(ReportRequestErrorStatus.REPORT_REQUEST_NOT_FOUND);

    public ReportRequestHandler(BaseErrorCode baseErrorCode) {
        super(baseErrorCode);
    }
}
