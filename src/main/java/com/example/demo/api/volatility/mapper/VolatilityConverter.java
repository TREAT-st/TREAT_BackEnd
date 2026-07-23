package com.example.demo.api.volatility.mapper;

import com.example.demo.api.volatility.dto.VolatilityResponseDto.VolatilityInfo;
import com.example.demo.api.volatility.dto.VolatilityResponseDto.VolatilityListResponse;
import com.example.demo.domain.volatility.entity.Volatility;

import java.util.List;
import java.util.stream.Collectors;

public class VolatilityConverter {

    public static VolatilityInfo toVolatilityInfo(Volatility volatility) {
        return VolatilityInfo.builder()
                .volatilityCode(volatility.getStockCode())
                .volatilityName(volatility.getStockName())
                .reportUrl(volatility.getReportUrl())
                .build();
    }

    public static VolatilityListResponse toVolatilityListResponse(List<Volatility> volatilities) {
        List<VolatilityInfo> content = volatilities.stream()
                .map(VolatilityConverter::toVolatilityInfo)
                .collect(Collectors.toList());

        return VolatilityListResponse.builder()
                .content(content)
                .totalCount(content.size())
                .build();
    }
}
