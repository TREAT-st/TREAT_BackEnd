package com.example.demo.infra.S3.exception;

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
public enum S3ErrorStatus implements BaseErrorCode {

    // Infra S3(5050~5099)
    S3_FILE_NOT_FOUND(HttpStatus.NOT_FOUND, 5050, "요청한 리포트 파일을 찾을 수 없습니다."),
    S3_ACCESS_DENIED(HttpStatus.FORBIDDEN, 5051, "S3 접근 권한이 없습니다."),
    S3_INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, 5052, "S3 처리 중 오류가 발생했습니다.");

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