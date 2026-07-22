package com.example.demo.api.volatility.service;

import com.example.demo.api.volatility.dto.VolatilityResponseDto.VolatilityInfo;
import com.example.demo.api.volatility.dto.VolatilityResponseDto.VolatilityListResponse;
import com.example.demo.api.volatility.mapper.VolatilityConverter;
import com.example.demo.common.annotation.UseCase;
import com.example.demo.domain.volatility.service.VolatilityQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@Transactional
@RequiredArgsConstructor
public class VolatilityUseCase {
    private final VolatilityQueryService volatilityQueryService;

    @Transactional(readOnly = true)
    public VolatilityListResponse getAllVolatility() {
        return VolatilityConverter.toVolatilityListResponse(volatilityQueryService.getAllVolatility());
    }

    @Transactional(readOnly = true)
    public VolatilityInfo getVolatilityByCode(String volatilityCode) {
        return VolatilityConverter.toVolatilityInfo(volatilityQueryService.getVolatilityByCode(volatilityCode));
    }
}
