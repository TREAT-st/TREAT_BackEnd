package com.example.demo.domain.volatility.port;

import com.example.demo.domain.volatility.entity.MarketOhlcv;

/**
 * 변동성 탐지에 필요한 시세 조회 포트. 도메인이 KRX 연동(api 계층)을 직접 알지 않게 한다.
 * 구현은 api/krx/adapter에 있다.
 */
public interface MarketOhlcvPort {

    /** 시총 상위 topN 종목의 최근 거래일 OHLCV. 받은 종목이 하나도 없으면 구현이 예외를 던진다. */
    MarketOhlcv getTopMarketCapOhlcv(int topN);
}
