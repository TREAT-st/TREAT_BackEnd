package com.example.demo.domain.stock.exception;

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
public enum StockErrorStatus implements BaseErrorCode {

    //  Entity Stock(4250~4299)
    //  KIS 연동 오류는 KisErrorStatus(4400~4449)로 분리했다.
    STOCK_NOT_FOUND(HttpStatus.NOT_FOUND, 4250, "stock을 찾지 못 했습니다."),
    S3_FILE_IO_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, 4251, "S3 파일 처리 중 오류가 발생했습니다."),
    INVALID_FILE(HttpStatus.BAD_REQUEST, 4252, "유효하지 않은 파일입니다. 비어있거나 .xlsx 파일이 아닙니다."),
    FILE_TOO_LARGE(HttpStatus.BAD_REQUEST, 4253, "파일의 크기가 너무 큽니다."),
    STOCK_IS_EMPTY(HttpStatus.NOT_FOUND, 4254, "종목 정보가 비어있습니다.");

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

