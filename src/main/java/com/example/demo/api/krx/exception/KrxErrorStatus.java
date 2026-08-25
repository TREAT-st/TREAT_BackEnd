package com.example.demo.api.krx.exception;

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
public enum KrxErrorStatus implements BaseErrorCode {

    // KRX 연동(4450~4499)
    @ExplainError("Lambda 호출 자체가 실패했습니다. 네트워크·IAM 권한·스로틀링 등을 확인하세요.")
    KRX_LAMBDA_INVOKE_ERROR(HttpStatus.BAD_GATEWAY, 4450, "KRX 종목 조회 Lambda 호출에 실패했습니다."),
    @ExplainError("Lambda는 호출됐으나 조회된 종목이 없습니다. 휴장일이거나 Lambda 내부 필터링 결과가 비어 있을 수 있습니다.")
    KRX_LAMBDA_RESPONSE_EMPTY(HttpStatus.BAD_GATEWAY, 4451, "KRX 종목 조회 Lambda 응답이 비어있습니다."),
    @ExplainError("Lambda는 호출됐으나 함수 내부에서 예외가 발생했습니다. Lambda 로그를 확인하세요.")
    KRX_LAMBDA_EXECUTION_ERROR(HttpStatus.BAD_GATEWAY, 4452, "KRX 종목 조회 Lambda 실행 중 오류가 발생했습니다."),
    @ExplainError("Lambda 응답 형식이 KrxOhlcvResponseDto와 맞지 않습니다. 양쪽 계약을 확인하세요.")
    KRX_LAMBDA_RESPONSE_PARSE_ERROR(HttpStatus.BAD_GATEWAY, 4453, "KRX 종목 조회 Lambda 응답을 해석하지 못했습니다.");

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
