package com.example.demo.api.volatility.mapper;

import com.example.demo.api.volatility.dto.VolatilityResponseDto.VolatilityInfo;
import com.example.demo.api.volatility.dto.VolatilityResponseDto.VolatilityListResponse;
import com.example.demo.domain.volatility.entity.Volatility;

import java.util.List;
import java.util.stream.Collectors;

public class VolatilityConverter {

    public static VolatilityInfo toVolatilityInfo(Volatility volatility) {
        return VolatilityInfo.builder()
                .stockCode(volatility.getStockCode())
                .stockName(volatility.getStockName())
                .reportUrl(volatility.getReportUrl())
                .createdDate(volatility.getCreatedDate())
                .build();
    }

    public static VolatilityListResponse toVolatilityListResponse(List<Volatility> volatilises) {
        List<VolatilityInfo> content = volatilises.stream()
                .map(VolatilityConverter::toVolatilityInfo)
                .collect(Collectors.toList());

        return VolatilityListResponse.builder()
                .content(content)
                .totalCount(content.size())
                .build();
    }
}
