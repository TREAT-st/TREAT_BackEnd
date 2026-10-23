package com.example.demo.domain.stockDailyPrice.exception;

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
public enum StockDailyPriceErrorStatus implements BaseErrorCode {

    //  Entity StockDailyPrice. 처음 stock 도메인에서 만든 코드라 기존 응답 코드를 유지하려고 stock 범위(4250~4299)의 번호를 그대로 쓴다.
    @ExplainError("from은 to보다 늦을 수 없고, 일봉을 받는 기간의 끝은 시세가 반영된 가장 최근 거래일 이하여야 합니다. "
            + "그보다 새로운 날짜는 아직 확정된 일봉이 없습니다. 일봉 삭제는 from·to가 모두 필요합니다.")
    DAILY_PRICE_INVALID_RANGE(HttpStatus.BAD_REQUEST, 4255,
            "일봉 기간이 올바르지 않습니다."),
    @ExplainError("일봉을 받는 기간의 끝은 시세가 반영된 최근 거래일입니다. 아직 반영된 시세가 없으면 기준일을 정할 수 없습니다. "
            + "먼저 코스피200 동기화를 실행하세요.")
    DAILY_PRICE_NOT_SYNCED_YET(HttpStatus.CONFLICT, 4256,
            "아직 반영된 시세가 없어 일봉을 받을 수 없습니다."),
    @ExplainError("요청한 종목코드 중 6자리 숫자가 아니거나 stock에 없는 코드가 있습니다. "
            + "조용히 건너뛰면 오타가 성공처럼 보이므로 요청 전체를 거절합니다. 어떤 코드인지는 서버 로그에 남습니다.")
    DAILY_PRICE_UNKNOWN_STOCK_CODES(HttpStatus.BAD_REQUEST, 4257,
            "존재하지 않거나 형식이 올바르지 않은 종목코드가 있습니다."),
    @ExplainError("백필이나 일일 적재가 어느 서버에서든 돌고 있습니다. 적재 중에 지우면 지운 데이터가 다시 들어가거나 "
            + "이력과 데이터가 어긋날 수 있어 거절합니다. 적재가 끝난 뒤 다시 요청하세요.")
    DAILY_PRICE_JOB_RUNNING(HttpStatus.CONFLICT, 4258,
            "일봉 적재가 진행 중이라 삭제할 수 없습니다.");

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

