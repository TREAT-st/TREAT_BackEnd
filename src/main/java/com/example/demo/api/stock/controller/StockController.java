package com.example.demo.api.stock.controller;

import com.example.demo.api.common.dto.ApiResponseDto;
import com.example.demo.api.stock.dto.StockResponseDto.StockPriceResponse;
import com.example.demo.api.stock.service.StockUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "[주식] 주식 종목 조회 API")
@RestController
@RequestMapping("/api/v1/stocks")
@RequiredArgsConstructor
public class StockController {
    private final StockUseCase stockUseCase;

    @Operation(summary = "전일(가장 최근 일자) 시가/종가 조회", description = "종목코드로 전날의(가장 최근 날짜) 시가와 종가를 KIS API에서 조회하고 DB에 저장합니다.")
    @GetMapping("/{stockCode}/today-price")
    public ApiResponseDto<StockPriceResponse> getLatestStockPrice(@PathVariable String stockCode) {
        return ApiResponseDto.onSuccess(stockUseCase.getLatestStockPriceByStockCode(stockCode));
    }
}
