package com.example.demo.api.stock.controller;

import com.example.demo.api.common.dto.ApiResponseDto;
import com.example.demo.api.stock.dto.StockResponseDto.*;
import com.example.demo.api.stock.mapper.StockConverter;
import com.example.demo.api.stock.service.StockUseCase;
import com.example.demo.domain.stock.entity.Stock;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


@Tag(name = "[주식] 주식 종목 API.")
@Validated
@RestController
@RequestMapping("/api/v1/stocks")
@RequiredArgsConstructor
public class StockController {

    private final StockUseCase stockUseCase;

    @Operation(summary = "코스피200 종목·시세 동기화 (KRX)",
            description = "KRX Lambda로 코스피200 구성종목과 가장 가까운 거래일의 시가/종가를 한 번에 받아 반영합니다.<br>" +
                    "편출된 종목은 삭제하지 않고 비활성 처리하며, 재편입되면 다시 활성으로 되돌립니다.")
    @PostMapping("/sync")
    public ApiResponseDto<SyncStocksResponse> syncKospi200FromKrx() {
        return ApiResponseDto.onSuccess(stockUseCase.syncKospi200FromKrx());
    }

    @Operation(summary = "종목 코드로 해당 종목 조회",
            description = "종목 코드로 코스피200 종목의 시가/종가를 조회합니다.<br>" +
                    "tradeDate는 해당 시세의 기준 거래일(당일과 가장 가까운 거래일)이며, 동기화 전이면 시세가 null일 수 있습니다.<br>" +
                    "편출된 종목도 조회되며 isActive로 현재 편입 여부를 알 수 있습니다.")
    @GetMapping("/{stockCode}")
    public ApiResponseDto<StockOpenAndClosePriceResponse> getStockByStockcode(
            @PathVariable @Pattern(regexp = "\\d{6}", message = "종목코드는 6자리 숫자입니다.") String stockCode) {
        return ApiResponseDto.onSuccess(stockUseCase.getStockOpenAndClosePrice(stockCode));
    }

    @Operation(summary = "모든 종목 조회", description = "Stock에 저장된 모든 종목의 코드와 이름을 페이지 단위로 조회합니다.")
    @GetMapping("/all-stock")
    public ApiResponseDto<StockPageResponse> getAllStocks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        Pageable pageable = PageRequest.of(page, pageSize, Sort.by(Sort.Order.asc("stockCode")));
        Page<Stock> stockPage = stockUseCase.getAllStocks(pageable);
        return ApiResponseDto.onSuccess(StockConverter.toStockPageResponse(stockPage));
    }
}
