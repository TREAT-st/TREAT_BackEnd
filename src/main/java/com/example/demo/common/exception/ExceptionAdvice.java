package com.example.demo.common.exception;

import com.example.demo.api.common.dto.ApiResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class ExceptionAdvice {

    @ExceptionHandler(GeneralException.class)
    public ResponseEntity<ApiResponseDto<?>> handleGeneralException(
            GeneralException e
    ) {

        Reason reason = e.getErrorReasonHttpStatus();

        return ResponseEntity
                .status(reason.getHttpStatus())
                .body(ApiResponseDto.onFailure(
                        reason.getCode(),
                        reason.getMessage(),
                        null
                ));
    }

    /** @Valid 검증 실패. 어떤 필드가 왜 틀렸는지 result에 담아 돌려준다. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponseDto<?>> handleMethodArgumentNotValid(
            MethodArgumentNotValidException e
    ) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
            fieldErrors.merge(fieldError.getField(), fieldError.getDefaultMessage(),
                    (existing, added) -> existing + ", " + added);
        }

        Reason reason = ErrorStatus._BAD_REQUEST.getReasonHttpStatus();

        return ResponseEntity
                .status(reason.getHttpStatus())
                .body(ApiResponseDto.onFailure(
                        reason.getCode(),
                        reason.getMessage(),
                        fieldErrors
                ));
    }

    /**
     * 위에서 잡히지 않은 모든 예외.
     * Spring MVC가 이미 상태코드를 정한 예외(404, 405, 415 등)는 그 상태를 보존하고,
     * 그 외에는 500으로 처리하되 내부 메시지를 노출하지 않고 로그로만 남긴다.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponseDto<?>> handleUnexpectedException(
            Exception e
    ) {
        if (e instanceof ErrorResponse errorResponse) {
            HttpStatusCode status = errorResponse.getStatusCode();
            log.warn("Spring MVC 예외 발생. status={}, type={}", status, e.getClass().getSimpleName());
            return ResponseEntity
                    .status(status)
                    .body(ApiResponseDto.onFailure(status.value(), e.getMessage(), null));
        }

        log.error("처리되지 않은 예외 발생", e);

        Reason reason = ErrorStatus._INTERNAL_SERVER_ERROR.getReasonHttpStatus();

        return ResponseEntity
                .status(reason.getHttpStatus())
                .body(ApiResponseDto.onFailure(
                        reason.getCode(),
                        reason.getMessage(),
                        null
                ));
    }
}
