package com.example.demo.domain.userDevice.exception;

import com.example.demo.common.exception.GeneralException;

public class UserDeviceHandler extends GeneralException {
    public static final GeneralException CONFLICT = new UserDeviceHandler();

    private UserDeviceHandler() {
        super(UserDeviceErrorStatus.DEVICE_CONFLICT);
    }
}
