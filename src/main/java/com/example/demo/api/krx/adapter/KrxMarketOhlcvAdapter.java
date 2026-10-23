package com.example.demo.api.krx.adapter;

import com.example.demo.api.krx.dto.KrxOhlcvResponseDto;
import com.example.demo.api.krx.service.KrxService;
import com.example.demo.domain.volatility.entity.MarketOhlcv;
import com.example.demo.domain.volatility.port.MarketOhlcvPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

import static com.example.demo.common.consts.StaticVariable.YYYYMMDD_FORMATTER;

/**
 * KRX Lambda(krxOhlcvFetcher) 응답을 변동성 도메인의 입력으로 바꾼다.
 * 응답의 존재와 거래일 형식은 KrxService가 경계에서 이미 검증했다.
 */
@Component
@RequiredArgsConstructor
public class KrxMarketOhlcvAdapter implements MarketOhlcvPort {

    private final KrxService krxService;

    @Override
    public MarketOhlcv getTopMarketCapOhlcv(int topN) {
        KrxOhlcvResponseDto response = krxService.getTopMarketCapOhlcv(topN);
        return new MarketOhlcv(
                LocalDate.parse(response.getEffectiveTradeDate(), YYYYMMDD_FORMATTER),
                response.getUniverseSize(),
                response.getStocks().stream()
                        .map(s -> new MarketOhlcv.StockSeries(s.getStockCode(), s.getStockName(), s.getMarketCapRank(),
                                s.getClose(), s.getHigh(), s.getLow(), s.getVolume()))
                        .toList());
    }
}
