package com.example.demo.domain.notification.exception;

import com.example.demo.common.exception.BaseErrorCode;
import com.example.demo.common.exception.GeneralException;

public class NotificationHandler extends GeneralException {
    public static final GeneralException NOT_FOUND
            = new NotificationHandler(NotificationErrorStatus.NOTIFICATION_NOT_FOUND);

    public NotificationHandler(BaseErrorCode baseErrorCode) {
        super(baseErrorCode);
    }
}
