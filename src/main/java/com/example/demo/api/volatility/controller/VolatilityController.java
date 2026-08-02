package com.example.demo.api.volatility.controller;

import com.example.demo.api.common.dto.ApiResponseDto;
import com.example.demo.api.volatility.dto.VolatilityResponseDto.VolatilityListResponse;
import com.example.demo.api.volatility.service.VolatilityUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Tag(name = "[변동성 종목 페이지] 변동성 종목 API")
@RestController
@RequestMapping("/api/v1/volatility")
@RequiredArgsConstructor
public class VolatilityController {
    private final VolatilityUseCase volatilityUseCase;

    @Operation(summary = "변동성 탐지 실행", description = "KRX Lambda에서 OHLCV를 조회하고 변동성을 계산하여 변동성이 탐지된 종목 결과를 저장합니다.")
    @PostMapping("/detect")
    public ApiResponseDto<Void> runDetection() {
        volatilityUseCase.runDetection();
        return ApiResponseDto.onSuccess(null);
    }

    @Operation(summary = "특정 날짜의 변동성 종목 전체 조회", description = "특정 날짜에 생성된 변동성 종목 전체를 조회합니다. 날짜 입력 형식은 \"yyyy-mm-dd\"입니다.")
    @GetMapping("/volatilty-by-date")
    public ApiResponseDto<VolatilityListResponse> getAllVolatilityByDate(@RequestParam LocalDate date) {
        return ApiResponseDto.onSuccess(volatilityUseCase.getAllVolatilityByDate(date));
    }

    @Operation(summary = "종목 코드로 변동성 전체 조회", description = "종목 코드로 해당 종목의 변동성 기록 전체를 조회합니다.")
    @GetMapping("/{stockCode}")
    public ApiResponseDto<VolatilityListResponse> getVolatilityByStockCode(@PathVariable String stockCode) {
        return ApiResponseDto.onSuccess(volatilityUseCase.getAllVolatilityByCode(stockCode));
    }
}
