package com.example.demo.domain.volatility.exception;

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
public enum VolatilityErrorStatus implements BaseErrorCode {

    // Entity Volatility(4300~4349)
    VOLATILITY_NOT_FOUND(HttpStatus.NOT_FOUND, 4300, "volatility를 찾지 못했습니다."),
    KRX_LAMBDA_INVOKE_ERROR(HttpStatus.BAD_GATEWAY, 4301, "KRX 종목 조회 Lambda 호출에 실패했습니다."),
    KRX_LAMBDA_RESPONSE_EMPTY(HttpStatus.BAD_GATEWAY, 4302, "KRX 종목 조회 Lambda 응답이 비어있습니다."),

    @ExplainError("오늘 /detect를 실행하지 않아 리포트를 생성할 대상 종목이 없습니다.")
    VOLATILITY_NOT_DETECTED_TODAY(HttpStatus.BAD_REQUEST, 4303, "오늘 탐지된 변동성 종목이 없습니다. 먼저 변동성 탐지를 실행해주세요."),
    REPORT_LAMBDA_INVOKE_ERROR(HttpStatus.BAD_GATEWAY, 4304, "리포트 생성 Lambda 호출에 실패했습니다."),
    @ExplainError("콜백 시크릿 헤더(X-Callback-Secret)가 없거나 일치하지 않습니다.")
    REPORT_CALLBACK_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, 4305, "리포트 콜백 인증에 실패했습니다."),
    REPORT_CALLBACK_INVALID_REQUEST(HttpStatus.BAD_REQUEST, 4306, "리포트 콜백 요청 형식이 올바르지 않습니다.");

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
