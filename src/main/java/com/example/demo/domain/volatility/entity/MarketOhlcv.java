package com.example.demo.domain.volatility.entity;

import java.time.LocalDate;
import java.util.List;

/**
 * 변동성 탐지의 입력. 시총 상위 종목의 최근 거래일 OHLCV 시계열이다.
 * 어디서 받아왔는지(KRX Lambda)는 MarketOhlcvPort 구현이 감춘다.
 *
 * @param tradeDate    시계열의 마지막 거래일. 탐지 결과와 리포트의 기준일이 된다.
 *                     서버 날짜로 대체하지 않는다. 대체하면 휴장일이나 응답 오류일 때 실제 거래일과 다른
 *                     Volatility와 리포트가 만들어진다
 * @param universeSize 시총 순위의 모수. 시총 가중치의 분모로 쓴다(0 이하면 알 수 없음)
 */
public record MarketOhlcv(LocalDate tradeDate, int universeSize, List<StockSeries> stocks) {

    /** 배열은 오래된 거래일부터 최신 순이다. */
    public record StockSeries(String stockCode, String stockName, int marketCapRank,
                              double[] close, double[] high, double[] low, double[] volume) {
    }
}
