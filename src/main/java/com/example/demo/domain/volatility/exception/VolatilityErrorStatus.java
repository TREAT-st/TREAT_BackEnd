package com.example.demo.domain.volatility.exception;

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
public enum VolatilityErrorStatus implements BaseErrorCode {

    // Entity Volatility(4300~4349)
    // KRX 연동 오류는 KrxErrorStatus(4400~4449)로 분리했다.
    VOLATILITY_NOT_FOUND(HttpStatus.NOT_FOUND, 4300, "volatility를 찾지 못했습니다."),
    @ExplainError("리포트를 생성할 탐지 결과가 없습니다. 수동 요청은 탐지 기록이 있는 가장 최근 거래일을, "
            + "배치는 지정한 거래일을 대상으로 삼습니다. 탐지는 실행일이 아니라 직전 거래일 기준이므로 "
            + "'오늘' 탐지 기록은 생기지 않습니다. /detect를 먼저 실행하세요.")
    VOLATILITY_NOT_DETECTED(HttpStatus.BAD_REQUEST, 4301, "리포트를 생성할 탐지된 변동성 종목이 없습니다. 먼저 변동성 탐지를 실행해주세요."),
    @ExplainError("Lambda 호출 자체가 실패했습니다. 네트워크·IAM 권한·스로틀링 등을 확인하세요.")
    REPORT_LAMBDA_INVOKE_ERROR(HttpStatus.BAD_GATEWAY, 4302, "리포트 생성 Lambda 호출에 실패했습니다."),
    @ExplainError("콜백 시크릿 헤더(X-Callback-Secret)가 없거나 일치하지 않습니다.")
    REPORT_CALLBACK_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, 4303, "리포트 콜백 인증에 실패했습니다."),
    REPORT_CALLBACK_INVALID_REQUEST(HttpStatus.BAD_REQUEST, 4304, "리포트 콜백 요청 형식이 올바르지 않습니다."),
    @ExplainError("Lambda는 호출됐으나 함수 내부에서 예외가 발생했습니다. Lambda 로그를 확인하세요.")
    REPORT_LAMBDA_EXECUTION_ERROR(HttpStatus.BAD_GATEWAY, 4305, "리포트 생성 Lambda 실행 중 오류가 발생했습니다."),
    @ExplainError("조회된 종목이 모두 지표 계산에 실패했습니다. 거래일 데이터가 부족하거나 응답 형식이 바뀌었을 수 있습니다.")
    VOLATILITY_DETECTION_FAILED(HttpStatus.BAD_GATEWAY, 4306, "변동성 분석에 실패했습니다. 분석 가능한 종목이 없습니다."),
    @ExplainError("시세 동기화 Lambda와 변동성 탐지 Lambda가 서로 다른 거래일을 반환했습니다. "
            + "두 Lambda의 거래일 판정 규칙(오늘 제외 여부)이 같은지 확인하세요. 탐지 결과는 저장하지 않았습니다.")
    VOLATILITY_TRADE_DATE_MISMATCH(HttpStatus.BAD_GATEWAY, 4307, "탐지 거래일이 동기화 거래일과 다릅니다."),
    @ExplainError("해당 날짜의 탐지 기록이 없습니다. 주말·공휴일 등 휴장일이거나 아직 탐지를 실행하지 않은 날짜입니다. "
            + "휴장일 달력이 없어 두 경우는 구분하지 않습니다. "
            + "배치가 탐지했는데 알림 대상이 0건이었던 거래일은 이 에러가 아니라 200과 빈 목록으로 응답합니다.")
    VOLATILITY_NOT_FOUND_BY_DATE(HttpStatus.NOT_FOUND, 4308, "해당 날짜에 탐지된 변동성 종목이 없습니다."),
    @ExplainError("해당 종목이 변동성 상위 종목으로 선정된 적이 없습니다. "
            + "종목 코드가 잘못됐거나, 코스피200 종목이지만 아직 한 번도 탐지되지 않은 경우입니다.")
    VOLATILITY_NOT_FOUND_BY_STOCK_CODE(HttpStatus.NOT_FOUND, 4309, "해당 종목의 변동성 기록이 없습니다."),
    @ExplainError("탐지는 장이 끝난 직전 거래일을 대상으로 하므로 오늘이나 미래 날짜의 탐지 기록은 생기지 않습니다.")
    VOLATILITY_DATE_NOT_YET_DETECTABLE(HttpStatus.BAD_REQUEST, 4310, "아직 탐지 대상이 아닌 날짜입니다.");

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
