package com.example.demo.domain.stockDailyPrice.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 일봉 한 건. Lambda 응답을 저장 서비스로 넘기는 입력이다. 엔티티를 바로 만들지 않아 upsert 판단을 서비스에 모은다.
 * 고가·저가는 못 받을 수 있어 null을 허용한다.
 */
public record DailyPriceRow(
        String stockCode,
        LocalDate tradeDate,
        BigDecimal openPrice,
        BigDecimal highPrice,
        BigDecimal lowPrice,
        BigDecimal closePrice
) {

    /**
     * 저장해도 되는 일봉인지. 시가·종가는 필수이고 0보다 커야 한다. 고가·저가는 없으면 null로 두되, 있으면 0보다 커야 한다.
     * 0원 봉은 거래정지일 같은 가짜 시세라 차트를 0까지 꺾는다.
     *
     * 저장 서비스와 백필 유스케이스가 같은 규칙을 쓴다. 백필은 이 규칙을 통과한 행이 하나라도 있어야 그 종목을 받은 것으로 본다.
     */
    public boolean isValid() {
        return stockCode != null && tradeDate != null
                && isPositive(openPrice) && isPositive(closePrice)
                && (highPrice == null || isPositive(highPrice))
                && (lowPrice == null || isPositive(lowPrice));
    }

    private static boolean isPositive(BigDecimal value) {
        return value != null && value.signum() > 0;
    }
}
