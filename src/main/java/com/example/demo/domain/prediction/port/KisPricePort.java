package com.example.demo.domain.prediction.port;

import java.math.BigDecimal;

public interface KisPricePort {
    /**
     * 종목 코드에 해당하는 현재가를 조회합니다.
     * TODO: KIS API 403 이슈 해결 후 실제 KIS 현재가 API 연동으로 교체
     */
    BigDecimal getCurrentPrice(String stockCode);
}