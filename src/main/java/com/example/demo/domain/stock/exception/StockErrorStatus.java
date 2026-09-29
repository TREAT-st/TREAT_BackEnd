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
            "편출 판정 종목이 비정상적으로 많아 동기화를 중단했습니다."),
    @ExplainError("KRX가 반환한 거래일의 데이터가 이미 반영되어 있습니다. "
            + "주말·휴장일에는 직전 거래일이 다시 내려오므로 정상적인 상황입니다. "
            + "시세를 못 받은 종목을 복구하려면 force=true로 재실행하세요.")
    STOCK_TRADE_DATE_ALREADY_SYNCED(HttpStatus.CONFLICT, 4252,
            "해당 거래일의 데이터가 이미 존재합니다."),
    @ExplainError("이미 더 최신 거래일이 반영돼 있어 과거 시세가 덮어쓰는 것을 막았습니다. "
            + "force로도 허용하지 않습니다.")
    STOCK_STALE_TRADE_DATE(HttpStatus.CONFLICT, 4253,
            "더 최신 거래일의 데이터가 존재하여 과거 데이터 동기화를 중단했습니다."),
    @ExplainError("Lambda는 항상 오늘을 제외한 직전 거래일을 반환해야 합니다. "
            + "오늘 이후 날짜나 빈 값이 오면 Lambda 응답이 깨진 것입니다.")
    STOCK_INVALID_TRADE_DATE(HttpStatus.BAD_GATEWAY, 4254,
            "KRX가 유효하지 않은 거래일을 반환했습니다.");

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

