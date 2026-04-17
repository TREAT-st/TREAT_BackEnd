package com.example.demo.domain.report.exception;

import com.example.demo.common.exception.BaseErrorCode;
import com.example.demo.common.exception.GeneralException;

public class ReportHandler extends GeneralException {
    public static final GeneralException NOT_FOUND
            = new ReportHandler(ReportErrorStatus.REPORT_NOT_FOUND);

    public static final GeneralException ALREADY_EXISTS
            = new ReportHandler(ReportErrorStatus.REPORT_ALREADY_EXISTS);

    public ReportHandler(BaseErrorCode baseErrorCode) {
        super(baseErrorCode);
    }
}
