package com.example.demo.api.volatility.controller;

import com.example.demo.api.common.dto.ApiResponseDto;
import com.example.demo.api.volatility.dto.VolatilityResponseDto.VolatilityInfo;
import com.example.demo.api.volatility.dto.VolatilityResponseDto.VolatilityListResponse;
import com.example.demo.api.volatility.service.VolatilityUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "[변동성 종목 페이지] 변동성 종목 API")
@RestController
@RequestMapping("/api/v1/volatility")
@RequiredArgsConstructor
public class VolatilityController {
    private final VolatilityUseCase volatilityUseCase;

    @Operation(summary = "변동성 종목 전체 조회", description = "저장된 변동성 종목 전체를 조회합니다.")
    @GetMapping
    public ApiResponseDto<VolatilityListResponse> getAllVolatility() {
        return ApiResponseDto.onSuccess(volatilityUseCase.getAllVolatility());
    }

    @Operation(summary = "변동성 종목 단건 조회", description = "종목 코드로 변동성 종목을 조회합니다.")
    @GetMapping("/{volatilityCode}")
    public ApiResponseDto<VolatilityInfo> getVolatilityByCode(@PathVariable String volatilityCode) {
        return ApiResponseDto.onSuccess(volatilityUseCase.getVolatilityByCode(volatilityCode));
    }
}
