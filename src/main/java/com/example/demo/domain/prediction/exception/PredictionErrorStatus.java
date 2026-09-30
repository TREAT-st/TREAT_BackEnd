package com.example.demo.domain.prediction.exception;

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
public enum PredictionErrorStatus implements BaseErrorCode {

    // Entity Prediction(4150~4199)
    PREDICTION_NOT_FOUND(HttpStatus.NOT_FOUND, 4150, "예측을 찾을 수 없습니다."),
    PREDICTION_ALREADY_GRADED(HttpStatus.BAD_REQUEST, 4151, "이미 채점된 예측입니다."),
    PREDICTION_NOT_MATURED(HttpStatus.BAD_REQUEST, 4152, "아직 만기가 도래하지 않은 예측입니다."),
    STOCK_PRICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, 4153, "종목 시세가 아직 동기화되지 않았습니다. 동기화 후 다시 시도해주세요."),
    PREDICTION_FORBIDDEN(HttpStatus.FORBIDDEN, 4154, "본인의 예측만 조회할 수 있습니다.");

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