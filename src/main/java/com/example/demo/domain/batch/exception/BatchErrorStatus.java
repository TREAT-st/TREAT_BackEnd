package com.example.demo.domain.batch.exception;

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
public enum BatchErrorStatus implements BaseErrorCode {

    //  배치 실행 이력(4450~4499)
    @ExplainError("실행 이력을 찾지 못했습니다. 기록 없이 완료 처리를 시도한 경우입니다.")
    BATCH_EXECUTION_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR, 4450, "배치 실행 이력을 찾지 못했습니다."),

    @ExplainError("X-Batch-Secret 헤더가 없거나 값이 다릅니다.")
    BATCH_TRIGGER_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, 4451, "배치 트리거 인증에 실패했습니다.");

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
