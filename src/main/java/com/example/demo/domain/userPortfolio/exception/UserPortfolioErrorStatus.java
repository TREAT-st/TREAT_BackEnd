package com.example.demo.domain.userPortfolio.exception;

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
public enum UserPortfolioErrorStatus implements BaseErrorCode {

    //  Entity UserPortfolio (4100~4149)
    USER_PORTFOLIO_NOT_FOUND(HttpStatus.NOT_FOUND, 4100, "user의 포트폴리오를 찾을 수 없습니다."),
    USER_PORTFOLIO_ALREADY_EXISTS(HttpStatus.CONFLICT , 4101, "user의 포트폴리오가 이미 존재합니다.");

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
