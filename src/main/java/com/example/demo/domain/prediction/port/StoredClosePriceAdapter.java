package com.example.demo.domain.prediction.port;

import com.example.demo.domain.prediction.exception.PredictionHandler;
import com.example.demo.domain.stock.entity.Stock;
import com.example.demo.domain.stock.service.StockQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 기준가 조회 구현. DB에 저장된 전일 종가(Stock.closePrice)를 돌려준다.
 * KIS 실시간 API 연동 전까지 쓰는 구현이다. 연동하면 CurrentPricePort의 다른 구현으로 교체한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StoredClosePriceAdapter implements CurrentPricePort {

    private final StockQueryService stockQueryService;

    @Override
    public BigDecimal getCurrentPrice(String stockCode) {
        Stock stock = stockQueryService.getStockByCode(stockCode);
        BigDecimal closePrice = stock.getClosePrice();

        if (closePrice == null) {
            log.error("[CurrentPrice] 종목 {} 의 시세가 동기화되지 않았습니다. (closePrice=null)", stockCode);
            throw PredictionHandler.STOCK_PRICE_UNAVAILABLE;
        }

        log.debug("[CurrentPrice] 종목 {} 전일 종가={} (기준일: {})", stockCode, closePrice, stock.getTradeDate());
        return closePrice;
    }
}