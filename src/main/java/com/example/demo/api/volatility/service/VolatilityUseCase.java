package com.example.demo.api.volatility.service;

import com.example.demo.api.volatility.dto.VolatilityResponseDto.VolatilityListResponse;
import com.example.demo.api.volatility.mapper.VolatilityConverter;
import com.example.demo.common.annotation.UseCase;
import com.example.demo.domain.volatility.service.VolatilityQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@UseCase
@Transactional
@RequiredArgsConstructor
public class VolatilityUseCase {
    private final VolatilityQueryService volatilityQueryService;

    @Transactional(readOnly = true)
    public VolatilityListResponse getAllVolatilityByDate(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();
        return VolatilityConverter.toVolatilityListResponse(volatilityQueryService.getAllVolatilityByDate(start, end));
    }

    @Transactional(readOnly = true)
    public VolatilityListResponse getAllVolatilityByCode(String stockCode) {
        return VolatilityConverter.toVolatilityListResponse(volatilityQueryService.getAllVolatilityByCode(stockCode));
    }
}
