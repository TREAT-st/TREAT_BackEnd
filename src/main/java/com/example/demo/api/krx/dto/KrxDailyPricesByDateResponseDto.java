package com.example.demo.api.krx.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * GetDailyPricesFromKrx 날짜 조회 모드({"date","tickers"}) 응답. 일일 적재에 쓴다.
 * 그 날짜의 시장 전체 시세를 한 번에 받아 요청한 종목만 걸러 온다. KRX 요청 1회다.
 *
 *   prices       : 가격이 있는 종목
 *   skipped      : 가격이 없는 것이 정상인 종목(거래정지, 그날 미상장·상장폐지). 실패가 아니다
 *   marketClosed : 시장 전체 시세가 없는 날(휴장일)
 * 요청했는데 어디에도 없는 종목은 서버가 실패로 친다. 호출 자체가 실패하면 Lambda가 예외를 낸다.
 */
@Getter
@NoArgsConstructor
public class KrxDailyPricesByDateResponseDto {

    /** yyyyMMdd */
    @JsonProperty("date")
    private String date;

    @JsonProperty("marketClosed")
    private boolean marketClosed;

    @JsonProperty("prices")
    private List<DailyPrice> prices;

    @JsonProperty("skipped")
    private List<KrxStockIssueDto> skipped;

    @Getter
    @NoArgsConstructor
    public static class DailyPrice {
        @JsonProperty("stockCode")
        private String stockCode;

        @JsonProperty("openPrice")
        private BigDecimal openPrice;

        @JsonProperty("highPrice")
        private BigDecimal highPrice;

        @JsonProperty("lowPrice")
        private BigDecimal lowPrice;

        @JsonProperty("closePrice")
        private BigDecimal closePrice;
    }
}
