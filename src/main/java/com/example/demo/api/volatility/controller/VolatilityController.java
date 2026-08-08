package com.example.demo.api.volatility.controller;

import com.example.demo.api.common.dto.ApiResponseDto;
import com.example.demo.api.volatility.dto.VolatilityRequestDto.ReportCallback;
import com.example.demo.api.volatility.dto.VolatilityRequestDto.ReportGenerationRequest;
import com.example.demo.api.volatility.dto.VolatilityRequestDto.SingleReportRequest;
import com.example.demo.api.volatility.dto.VolatilityResponseDto.DetectionResult;
import com.example.demo.api.volatility.dto.VolatilityResponseDto.ReportGenerationResult;
import com.example.demo.api.volatility.dto.VolatilityResponseDto.VolatilityListResponse;
import com.example.demo.api.volatility.service.VolatilityUseCase;
import com.example.demo.domain.volatility.exception.VolatilityHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;

@Tag(name = "[변동성 종목 페이지] 변동성 종목 API")
@RestController
@RequestMapping("/api/v1/volatility")
@RequiredArgsConstructor
public class VolatilityController {

    private static final String CALLBACK_SECRET_HEADER = "X-Callback-Secret";

    private final VolatilityUseCase volatilityUseCase;

    @Value("${report.callback.secret}")
    private String callbackSecret;

    @Operation(summary = "변동성 탐지 실행", description = "KRX Lambda에서 OHLCV를 조회하고 변동성을 계산하여 변동성이 탐지된 종목 결과를 저장합니다.")
    @PostMapping("/detect")
    public ApiResponseDto<DetectionResult> runDetection() {
        return ApiResponseDto.onSuccess(volatilityUseCase.runDetection());
    }

    @Operation(summary = "변동성 리포트 생성 요청", description = "오늘 탐지된 변동성 종목에 대한 리포트 생성을 Lambda에 요청합니다. 기본값으로 하고 싶으면 gptModel을 지우시면 됩니다.<br>" +
            "모델 목록: gpt-5.6-sol, gpt-5.6-terra(기본값), gpt-5.6-luna, gpt-5.5, gpt-5.5-pro, gpt-5.4, gpt-5.4-mini, gpt-5.4-nano, gpt-5.4-pro")
    @PostMapping(value = "/report", consumes = APPLICATION_JSON_VALUE)
    public ApiResponseDto<ReportGenerationResult> runReportGeneration(@RequestBody ReportGenerationRequest request) {
        return ApiResponseDto.onSuccess(volatilityUseCase.runReportGeneration(request));
    }

    @Operation(summary = "[테스트] 단일 종목 리포트 생성 요청", description = "테스트용. 종목 코드와 이름을 직접 입력해 Lambda에 리포트 생성을 요청합니다.<br>" +
            "모델 목록: gpt-5.6-sol, gpt-5.6-terra(기본값), gpt-5.6-luna, gpt-5.5, gpt-5.5-pro, gpt-5.4, gpt-5.4-mini, gpt-5.4-nano, gpt-5.4-pro")
    @PostMapping(value = "/report/test", consumes = APPLICATION_JSON_VALUE)
    public ApiResponseDto<Void> runSingleReportGeneration(@RequestBody @Valid SingleReportRequest request) {
        volatilityUseCase.runSingleReportGeneration(request);
        return ApiResponseDto.onSuccess(null);
    }

    @Operation(summary = "리포트 생성 완료 콜백", description = "ECS가 리포트 생성 완료 후 reportUrl을 전달합니다. " +
            "테스트에서는 이 api를 실행해야 S3에 생성 및 저장된 리포트가 DB에 저장됩니다.<br>" +
            "형식은 종목 코드, 생성 일자(파일명의 일자, yyyymmdd), S3 URL입니다.<br>" +
            "callback-secret은 문서의 REPORT_CALLBACK_SECRET 환경 변수 넣어주면 됩니다.")
    @PatchMapping(value = "/report/callback", consumes = APPLICATION_JSON_VALUE)
    public ApiResponseDto<Void> reportCallback(
            @RequestHeader(value = CALLBACK_SECRET_HEADER, required = false) String secret,
            @RequestBody @Valid ReportCallback request) {
        verifyCallbackSecret(secret);
        volatilityUseCase.handleReportCallback(request);
        return ApiResponseDto.onSuccess(null);
    }

    /** 콜백은 JWT 없이 열려 있으므로 공유 시크릿으로 검증한다. 타이밍 공격을 피해 상수 시간 비교를 쓴다. */
    private void verifyCallbackSecret(String secret) {
        if (secret == null || !MessageDigest.isEqual(
                secret.getBytes(StandardCharsets.UTF_8),
                callbackSecret.getBytes(StandardCharsets.UTF_8))) {
            throw VolatilityHandler.reportCallbackUnauthorized();
        }
    }

    @Operation(summary = "특정 날짜의 변동성 종목 전체 조회", description = "특정 날짜에 생성된 변동성 종목 전체를 조회합니다. 날짜 입력 형식은 \"yyyy-mm-dd\"입니다.")
    @GetMapping("/volatility-by-date")
    public ApiResponseDto<VolatilityListResponse> getAllVolatilityByDate(@RequestParam LocalDate date) {
        return ApiResponseDto.onSuccess(volatilityUseCase.getAllVolatilityByDate(date));
    }

    @Operation(summary = "종목 코드로 변동성 전체 조회", description = "종목 코드로 해당 종목의 변동성 기록 전체를 조회합니다.")
    @GetMapping("/{stockCode}")
    public ApiResponseDto<VolatilityListResponse> getVolatilityByStockCode(@PathVariable String stockCode) {
        return ApiResponseDto.onSuccess(volatilityUseCase.getAllVolatilityByCode(stockCode));
    }
}
