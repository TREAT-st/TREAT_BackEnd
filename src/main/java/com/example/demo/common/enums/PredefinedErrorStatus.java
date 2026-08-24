package com.example.demo.common.enums;

import com.example.demo.common.exception.ErrorStatus;

import java.util.List;

public enum PredefinedErrorStatus {
    DEFAULT(List.of(ErrorStatus._INTERNAL_SERVER_ERROR)),
    AUTH(List.of(
            ErrorStatus._INTERNAL_SERVER_ERROR,
            ErrorStatus._UNAUTHORIZED_LOGIN_DATA_RETRIEVAL_ERROR,
            ErrorStatus._ASSIGNABLE_PARAMETER,
            ErrorStatus.MEMBER_NOT_FOUND
    ));

    private final List<ErrorStatus> errorStatuses;

    PredefinedErrorStatus(List<ErrorStatus> errorStatuses) {
        this.errorStatuses = errorStatuses;
    }

    public List<ErrorStatus> getErrorStatuses() {
        return errorStatuses;
    }
}
