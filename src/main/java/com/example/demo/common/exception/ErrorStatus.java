package com.example.demo.common.exception;

import com.example.demo.common.annotation.ExplainError;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.lang.reflect.Field;
import java.util.Objects;

import static org.springframework.http.HttpStatus.*;

@Getter
@AllArgsConstructor
public enum ErrorStatus implements BaseErrorCode{

    // 서버 오류
    @ExplainError("500번대 알수없는 오류입니다. 서버 관리자에게 문의 주세요")
    _INTERNAL_SERVER_ERROR(INTERNAL_SERVER_ERROR, 5000, "서버 에러, 관리자에게 문의 바랍니다."),
    @ExplainError("인증이 필요없는 api입니다.")
    _UNAUTHORIZED_LOGIN_DATA_RETRIEVAL_ERROR(INTERNAL_SERVER_ERROR, 5001, "서버 에러, 로그인이 필요없는 요청입니다."),
    _ASSIGNABLE_PARAMETER(BAD_REQUEST, 5002, "인증타입이 잘못되어 할당이 불가능합니다."),

    // 일반적인 요청 오류
    _BAD_REQUEST(BAD_REQUEST, 4000, "잘못된 요청입니다."),
    _UNAUTHORIZED(UNAUTHORIZED, 4001, "로그인이 필요합니다."),
    _FORBIDDEN(FORBIDDEN, 4002, "금지된 요청입니다."),

    // 인증 관련 오류 (4350~4399)
    @ExplainError("카카오 로그인 시 이메일 동의를 하지 않아 이메일을 가져오지 못했습니다.")
    AUTH_OAUTH2_EMAIL_NOT_FOUND_FROM_PROVIDER(BAD_REQUEST, 4350, "카카오 계정에서 이메일을 가져올 수 없습니다. 이메일 제공 동의가 필요합니다."),
    @ExplainError("DB에 존재하지 않는 Refresh Token입니다. 재로그인이 필요합니다.")
    AUTH_REFRESH_TOKEN_NOT_FOUND(UNAUTHORIZED, 4351, "유효하지 않은 Refresh Token입니다. 다시 로그인해주세요."),
    @ExplainError("만료된 Refresh Token입니다. 재로그인이 필요합니다.")
    AUTH_REFRESH_TOKEN_EXPIRED(UNAUTHORIZED, 4352, "Refresh Token이 만료되었습니다. 다시 로그인해주세요."),
    @ExplainError("유효하지 않거나 변조된 Refresh Token입니다. 재로그인이 필요합니다.")
    AUTH_INVALID_REFRESH_TOKEN(UNAUTHORIZED, 4355, "Refresh Token이 유효하지 않습니다. 다시 로그인해주세요."),
    @ExplainError("해당 Role로는 접근할 수 없는 URI입니다.")
    AUTH_ROLE_CANNOT_EXECUTE_URI(FORBIDDEN, 4353, "접근 권한이 없습니다."),
    @ExplainError("인증이 필요한 URI입니다. Access Token을 헤더에 담아 요청해주세요.")
    AUTH_MUST_AUTHORIZED_URI(UNAUTHORIZED, 4354, "로그인이 필요한 서비스입니다."),
    @ExplainError("위조되었거나 잘못된 형식의 Access Token입니다.")
    AUTH_INVALID_TOKEN(UNAUTHORIZED, 4356, "유효하지 않은 토큰입니다."),
    @ExplainError("만료된 Access Token입니다. 재발급이 필요합니다.")
    AUTH_TOKEN_HAS_EXPIRED(UNAUTHORIZED, 4357, "만료된 토큰입니다. 재발급 후 사용해주세요."),
    @ExplainError("지원하지 않는 JWT 형식입니다.")
    AUTH_TOKEN_IS_UNSUPPORTED(UNAUTHORIZED, 4358, "지원하지 않는 토큰 형식입니다."),
    @ExplainError("토큰 값이 비어있습니다.")
    AUTH_IS_NULL(UNAUTHORIZED, 4359, "토큰이 존재하지 않습니다."),

    // member (4050~4099)
    @ExplainError("존재하지 않는 NameType 입니다.")
    MEMBER_NAME_TYPE_IS_INVALID(BAD_REQUEST, 4050, "유효하지 않은 이름 타입입니다."),
    @ExplainError("존재하지 않는 회원입니다.")
    MEMBER_NOT_FOUND(BAD_REQUEST, 4051, "존재하지 않는 회원입니다.");



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

