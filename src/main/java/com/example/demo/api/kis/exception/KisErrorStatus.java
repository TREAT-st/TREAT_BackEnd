package com.example.demo.api.kis.exception;

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
public enum KisErrorStatus implements BaseErrorCode {

    // KIS 연동(4400~4449)
    KIS_API_ERROR(HttpStatus.BAD_GATEWAY, 4400, "KIS API 호출에 실패했습니다."),
    @ExplainError("KIS가 응답은 했으나 조회 결과가 비어 있습니다. 휴장일이거나 거래 정지 종목일 수 있습니다.")
    KIS_RESPONSE_EMPTY(HttpStatus.BAD_GATEWAY, 4401, "KIS API 응답에 데이터가 없습니다.");

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
