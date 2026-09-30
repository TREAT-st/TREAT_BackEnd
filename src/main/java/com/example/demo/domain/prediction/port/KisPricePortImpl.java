package com.example.demo.domain.prediction.port;

import com.example.demo.domain.prediction.exception.PredictionHandler;
import com.example.demo.domain.stock.entity.Stock;
import com.example.demo.domain.stock.service.StockQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 현재가 조회 구현체.
 * KIS 실시간 API 연동 전까지 DB에 저장된 전일 종가(Stock.closePrice)를 기준가로 사용한다.
 *
 * TODO: KIS API 403 이슈 해결 후 실시간 현재가 조회로 교체
 *       - KIS API 연동 시 이 클래스만 수정하면 됨
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KisPricePortImpl implements KisPricePort {

    private final StockQueryService stockQueryService;

    @Override
    public BigDecimal getCurrentPrice(String stockCode) {
        Stock stock = stockQueryService.getStockByCode(stockCode);
        BigDecimal closePrice = stock.getClosePrice();

        if (closePrice == null) {
            log.error("[KisPricePort] 종목 {} 의 시세가 동기화되지 않았습니다. (closePrice=null)", stockCode);
            throw PredictionHandler.STOCK_PRICE_UNAVAILABLE;
        }

        log.debug("[KisPricePort] 종목 {} 전일 종가={} (기준일: {})", stockCode, closePrice, stock.getTradeDate());
        return closePrice;
    }
}