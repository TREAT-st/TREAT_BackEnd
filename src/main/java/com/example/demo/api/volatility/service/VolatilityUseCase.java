package com.example.demo.api.volatility.service;

import com.example.demo.api.volatility.dto.VolatilityResponseDto.VolatilityListResponse;
import com.example.demo.api.volatility.mapper.VolatilityConverter;
import com.example.demo.common.annotation.UseCase;
import com.example.demo.domain.volatility.service.VolatilityCommandService;
import com.example.demo.domain.volatility.service.VolatilityDetectionService;
import com.example.demo.domain.volatility.service.VolatilityQueryService;
import com.example.demo.domain.volatility.service.VolatilitySignal;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@UseCase
@Transactional
@RequiredArgsConstructor
public class VolatilityUseCase {
    private final VolatilityQueryService volatilityQueryService;
    private final VolatilityDetectionService volatilityDetectionService;
    private final VolatilityCommandService volatilityCommandService;

    public void runDetection() {
        List<VolatilitySignal> signals = volatilityDetectionService.detect(100);
        List<VolatilitySignal> top10 = volatilityDetectionService.selectTop(signals, 100, 10);
        volatilityCommandService.saveTopVolatilityStocks(top10);
    }

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
