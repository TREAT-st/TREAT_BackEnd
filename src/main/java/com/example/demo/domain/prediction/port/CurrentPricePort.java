package com.example.demo.domain.prediction.port;

import java.math.BigDecimal;

/**
 * 예측 채점에 쓰는 기준가 조회 포트.
 * 지금 구현은 DB에 저장된 전일 종가를 돌려준다(StoredClosePriceAdapter).
 * KIS 실시간 현재가를 연동하면 이 포트의 구현만 바꾼다.
 */
public interface CurrentPricePort {
    /**
     * 종목 코드에 해당하는 기준가를 조회합니다.
     * TODO: KIS API 403 이슈 해결 후 KIS 현재가 API를 구현으로 추가
     */
    BigDecimal getCurrentPrice(String stockCode);
}