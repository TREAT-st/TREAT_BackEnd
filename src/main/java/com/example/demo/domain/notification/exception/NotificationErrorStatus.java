package com.example.demo.domain.notification.exception;

import com.example.demo.common.annotation.ExplainError;
import com.example.demo.common.exception.BaseErrorCode;
import com.example.demo.common.exception.Reason;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.lang.reflect.Field;
import java.util.Objects;

@Getter
@AllArgsConstructor
public enum NotificationErrorStatus implements BaseErrorCode {

    //  Entity Notification(4150~4199)
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, 4150, "알림을 찾지 못 했습니다."),
    NOTIFICATION_FORBIDDEN(HttpStatus.FORBIDDEN, 4151, "해당 알림에 대한 권한이 없습니다.");

    private final HttpStatus httpStatus;
    private final Integer code;
    private final String message;

    @Override
    public Reason getReason() {
        return Reason.builder()
                .message(message)
                .code(code)
                .isSuccess(false)
                .build();
    }

    @Override
    public Reason getReasonHttpStatus() {
        return Reason.builder()
                .message(message)
                .code(code)
                .isSuccess(false)
                .httpStatus(httpStatus)
                .build();
    }

    @Override
    public String getExplainError() throws NoSuchFieldException {
        Field field = this.getClass().getField(this.name());
        ExplainError annotation = field.getAnnotation(ExplainError.class);
        return Objects.nonNull(annotation) ? annotation.value() : this.getMessage();
    }
}
