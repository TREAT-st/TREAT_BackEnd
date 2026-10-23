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
import jakarta.validation.constraints.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static com.example.demo.common.consts.StaticVariable.CALLBACK_SECRET_HEADER;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;

@Tag(name = "[변동성 종목 페이지] 변동성 종목 API")
@Validated
@RestController
@RequestMapping("/api/v1/volatility")
@RequiredArgsConstructor
public class VolatilityController {

    private final VolatilityUseCase volatilityUseCase;

    @Value("${report.callback.secret}")
    private String callbackSecret;

    @Operation(summary = "변동성 탐지 실행", description = "KRX Lambda에서 OHLCV를 조회하고 변동성을 계산하여 변동성이 탐지된 종목 결과를 저장합니다.")
    @PostMapping("/detect")
    public ApiResponseDto<DetectionResult> runDetection() {
        return ApiResponseDto.onSuccess(volatilityUseCase.runDetection());
    }

    @Operation(summary = "변동성 리포트 생성 요청", description = "탐지 기록이 있는 가장 최근 거래일의 변동성 종목에 대한 리포트 생성을 Lambda에 요청합니다.<br>" +
            "탐지는 실행일이 아니라 직전 거래일 기준이므로, 대상 거래일은 보통 오늘보다 앞선 날짜입니다. " +
            "대상 거래일은 응답의 tradeDate로 확인하세요.<br>" +
            "응답의 status는 SUCCESS(전부 요청됨), PARTIAL_SUCCESS(일부 실패), FAILURE(전부 실패) 중 하나입니다. " +
            "요청 성공은 생성 완료가 아니며, reportUrl은 리포트가 만들어진 뒤 콜백으로 채워집니다.<br>" +
            "기본 모델을 쓰려면 gptModel을 지우시면 됩니다.<br>" +
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

    private void verifyCallbackSecret(String secret) {
        if (secret == null || !MessageDigest.isEqual(
                secret.getBytes(StandardCharsets.UTF_8),
                callbackSecret.getBytes(StandardCharsets.UTF_8))) {
            throw VolatilityHandler.reportCallbackUnauthorized();
        }
    }

    @Operation(summary = "특정 날짜의 변동성 종목 전체 조회", description = "특정 거래일에 탐지된 변동성 종목 전체를 조회합니다. 날짜 입력 형식은 \"yyyy-MM-dd\"입니다.<br>" +
            "· 기록 있음 → 200, 목록<br>" +
            "· 배치가 탐지했지만 알림 대상이 0건인 거래일 → 200, 빈 목록 (조용한 장세의 정상 결과)<br>" +
            "· 오늘 또는 미래 날짜 → 400(4310). 탐지는 장이 끝난 직전 거래일 기준입니다<br>" +
            "· 그 외 → 404(4308). 주말·공휴일 등 휴장일이거나 아직 탐지하지 않은 날짜이며, 둘은 구분하지 않습니다<br>" +
            "수동 탐지(/detect)로 0건이 나온 날짜는 이력이 남지 않아 404로 응답합니다.")
    @GetMapping("/volatility-by-date")
    public ApiResponseDto<VolatilityListResponse> getAllVolatilityByDate(@RequestParam LocalDate date) {
        return ApiResponseDto.onSuccess(volatilityUseCase.getAllVolatilityByDate(date));
    }

    @Operation(summary = "종목 코드로 변동성 전체 조회", description = "종목 코드로 해당 종목의 변동성 기록 전체를 최신 거래일 순으로 조회합니다.<br>" +
            "종목코드가 6자리 숫자가 아니면 400, 변동성 상위 종목으로 선정된 적이 없는 종목이면 404(4309)를 반환합니다.")
    @GetMapping("/{stockCode}")
    public ApiResponseDto<VolatilityListResponse> getVolatilityByStockCode(
            @PathVariable @Pattern(regexp = "\\d{6}", message = "종목코드는 6자리 숫자입니다.") String stockCode) {
        return ApiResponseDto.onSuccess(volatilityUseCase.getAllVolatilityByCode(stockCode));
    }
}
