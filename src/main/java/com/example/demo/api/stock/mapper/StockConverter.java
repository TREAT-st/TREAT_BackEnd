package com.example.demo.api.stock.mapper;

import com.example.demo.api.stock.dto.StockResponseDto.StockItemResponse;
import com.example.demo.api.stock.dto.StockResponseDto.StockOpenAndClosePriceResponse;
import com.example.demo.api.stock.dto.StockResponseDto.StockPageResponse;
import com.example.demo.domain.stock.entity.Stock;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.stream.Collectors;

public class StockConverter {

    /**
     * 시가·종가는 동기화 전이면 null일 수 있다. tradeDate로 그 시세가 언제 것인지 판단한다.
     * isActive가 false면 코스피200에서 편출된 종목이라 시세가 더 이상 갱신되지 않는다.
     */
    public static StockOpenAndClosePriceResponse toStockOpenAndClosePriceResponse(Stock stock) {
        return StockOpenAndClosePriceResponse.builder()
                .stockCode(stock.getStockCode())
                .stockName(stock.getStockName())
                .openPrice(stock.getOpenPrice())
                .closePrice(stock.getClosePrice())
                .tradeDate(stock.getTradeDate())
                .isActive(stock.getIsActive())
                .build();
    }

    public static StockItemResponse toStockItemResponse(Stock stock) {
        return StockItemResponse.builder()
                .stockCode(stock.getStockCode())
                .stockName(stock.getStockName())
                .isActive(stock.getIsActive())
                .build();
    }

    public static List<StockItemResponse> toStockItemResponseList(List<Stock> stocks) {
        return stocks.stream()
                .map(StockConverter::toStockItemResponse)
                .collect(Collectors.toList());
    }

    public static StockPageResponse toStockPageResponse(Page<Stock> page) {
        List<StockItemResponse> content = toStockItemResponseList(page.getContent());
        return StockPageResponse.builder()
                .content(content)
                .page(page.getNumber())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .build();
    }
}
