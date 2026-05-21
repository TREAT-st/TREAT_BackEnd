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

    //  Entity Stock(4350~4399)
    STOCK_NOT_FOUND(HttpStatus.NOT_FOUND, 4350, "stock을 찾지 못 했습니다."),
    STOCK_ALREADY_EXISTS(HttpStatus.CONFLICT, 4351, "이미 등록된 stock입니다."),
    STOCK_PRICE_NOT_AVAILABLE(HttpStatus.BAD_GATEWAY, 4352, "시가/종가 데이터가 없습니다. 공휴일이거나 거래 정지 종목일 수 있습니다."),
    STOCK_PRICE_RESPONSE_EMPTY(HttpStatus.BAD_GATEWAY, 4353, "KIS API 응답에 데이터가 없습니다."),
    KIS_API_ERROR(HttpStatus.BAD_GATEWAY, 4354, "KIS API 호출에 실패했습니다."),
    S3_FILE_IO_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, 4355, "S3 파일 처리 중 오류가 발생했습니다."),
    INVALID_FILE(HttpStatus.BAD_REQUEST, 4356, "유효하지 않은 파일입니다. 비어있거나 .xlsx 파일이 아닙니다.");

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

