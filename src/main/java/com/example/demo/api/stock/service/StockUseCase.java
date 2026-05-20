package com.example.demo.api.stock.service;

import com.example.demo.api.kis.dto.KisResponseDto;
import com.example.demo.api.kis.service.KisService;
import com.example.demo.api.stock.dto.StockResponseDto.StockPriceResponse;
import com.example.demo.api.stock.mapper.StockConverter;
import com.example.demo.common.annotation.UseCase;
import com.example.demo.domain.stock.entity.Stock;
import com.example.demo.domain.stock.exception.StockHandler;
import com.example.demo.domain.stock.service.StockCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@UseCase
@RequiredArgsConstructor
public class StockUseCase {

    private final StockCommandService stockCommandService;
    private final KisService kisService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Transactional
    public StockPriceResponse getLatestStockPriceByStockCode(String stockCode) {
        for (int i = 1; i <= 10; i++) {
            LocalDate targetDate = LocalDate.now().minusDays(i);

            if (isWeekend(targetDate)) continue;

            String dateStr = targetDate.format(DATE_FORMATTER);
            KisResponseDto response = kisService.getDailyStockPrice(stockCode, dateStr, dateStr);

            List<KisResponseDto.DailyData> output2 = response.getOutput2();
            if (output2 == null || output2.isEmpty()) continue;

            String rawOpen  = output2.get(0).getOpenPrice();
            String rawClose = output2.get(0).getClosePrice();
            if (rawOpen == null || rawOpen.isBlank() || rawClose == null || rawClose.isBlank()) continue;

            BigDecimal openPrice  = new BigDecimal(rawOpen);
            BigDecimal closePrice = new BigDecimal(rawClose);

            Stock stock = stockCommandService.updateStockPrice(stockCode, openPrice, closePrice, targetDate);
            return StockConverter.toStockPriceResponse(stock);
        }

        throw StockHandler.stockPriceResponseEmpty();
    }

    private boolean isWeekend(LocalDate date) {
        return date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY;
    }
}
