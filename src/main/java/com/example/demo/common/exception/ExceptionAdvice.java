package com.example.demo.common.exception;

import com.example.demo.api.common.dto.ApiResponseDto;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
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

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponseDto<?>> handleUnreadableRequest(HttpMessageNotReadableException e) {
        Reason reason = ErrorStatus._BAD_REQUEST.getReasonHttpStatus();
        return ResponseEntity.status(reason.getHttpStatus())
                .body(ApiResponseDto.onFailure(reason.getCode(), reason.getMessage(), null));
    }

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
     * @Validated 컨트롤러의 파라미터 검증 실패(@PathVariable·@RequestParam의 @Pattern, @Min, @Max 등).
     *
     * 이 핸들러가 없으면 아래 catch-all로 빠져 500이 된다. ConstraintViolationException은
     * Spring MVC의 ErrorResponse가 아니라서 상태코드 보존 분기에도 걸리지 않는다.
     * 본문 검증(@Valid @RequestBody)과 같은 모양으로 파라미터 이름별 사유를 result에 담는다.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponseDto<?>> handleConstraintViolation(ConstraintViolationException e) {
        Map<String, String> violations = new LinkedHashMap<>();
        for (ConstraintViolation<?> violation : e.getConstraintViolations()) {
            violations.merge(parameterName(violation), violation.getMessage(),
                    (existing, added) -> existing + ", " + added);
        }

        Reason reason = ErrorStatus._BAD_REQUEST.getReasonHttpStatus();

        return ResponseEntity
                .status(reason.getHttpStatus())
                .body(ApiResponseDto.onFailure(
                        reason.getCode(),
                        reason.getMessage(),
                        violations
                ));
    }

    /** 경로는 "메서드명.파라미터명" 형태다. 클라이언트에게는 파라미터명만 의미가 있다. */
    private String parameterName(ConstraintViolation<?> violation) {
        String name = null;
        for (Path.Node node : violation.getPropertyPath()) {
            name = node.getName();
        }
        return name != null ? name : violation.getPropertyPath().toString();
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
