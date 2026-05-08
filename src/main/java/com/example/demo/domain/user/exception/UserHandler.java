package com.example.demo.domain.user.exception;

import com.example.demo.common.exception.BaseErrorCode;
import com.example.demo.common.exception.GeneralException;

public class UserHandler extends GeneralException {
    public static final GeneralException NOT_FOUND
            = new UserHandler(UserErrorStatus.USER_NOT_FOUND);

    public UserHandler(BaseErrorCode baseErrorCode) {
        super(baseErrorCode);
    }
}
