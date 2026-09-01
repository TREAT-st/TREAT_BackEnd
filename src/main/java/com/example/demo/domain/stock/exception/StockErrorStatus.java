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
    STOCK_NOT_FOUND(HttpStatus.NOT_FOUND, 4250, "stock을 찾지 못 했습니다."),
    @ExplainError("지수 구성종목 목록이 일부만 조회되면 나머지가 통째로 편출됩니다. "
            + "KRX Lambda 응답의 구성종목 수를 먼저 확인하세요.")
    STOCK_ABNORMAL_DEACTIVATION(HttpStatus.BAD_GATEWAY, 4251,
            "편출 판정 종목이 비정상적으로 많아 동기화를 중단했습니다.");

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

