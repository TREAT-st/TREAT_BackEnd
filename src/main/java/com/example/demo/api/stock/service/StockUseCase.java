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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@UseCase
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class StockUseCase {

    private final StockCommandService stockCommandService;
    private final KisService kisService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Transactional
    public StockPriceResponse getLatestStockPriceByStockCode(String stockCode) {
        LocalDate targetDate = getPreviousBusinessDay(LocalDate.now());
        String dateStr = targetDate.format(DATE_FORMATTER);

        KisResponseDto response = kisService.getDailyStockPrice(stockCode, dateStr, dateStr);

        List<KisResponseDto.DailyData> output2 = response.getOutput2();
        if (output2 == null || output2.isEmpty()) {
            throw StockHandler.STOCK_PRICE_RESPONSE_EMPTY;
        }

        String rawOpen  = output2.get(0).getOpenPrice();
        String rawClose = output2.get(0).getClosePrice();

        if (rawOpen == null || rawOpen.isBlank() || rawClose == null || rawClose.isBlank()) {
            throw StockHandler.STOCK_PRICE_NOT_AVAILABLE;
        }

        BigDecimal openPrice  = new BigDecimal(rawOpen);
        BigDecimal closePrice = new BigDecimal(rawClose);

        Stock stock = stockCommandService.updateStockPrice(stockCode, openPrice, closePrice, targetDate);

        return StockConverter.toStockPriceResponse(stock);
    }

    private LocalDate getPreviousBusinessDay(LocalDate date) {
        LocalDate previous = date.minusDays(1);
        return switch (previous.getDayOfWeek()) {
            case SATURDAY -> previous.minusDays(1);
            case SUNDAY   -> previous.minusDays(2);
            default       -> previous;
        };
    }
}
