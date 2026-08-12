package com.example.demo.api.krx.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 코스피200 구성종목과 해당 거래일의 시가·종가를 함께 내려주는 Lambda의 응답 계약.
 * 종목별 실패는 stocks에서 빠지고 errors에 사유가 담긴다(종목별 실패 격리).
 */
@Getter
@NoArgsConstructor
public class KrxKospi200ResponseDto {

    /** 실제 시세 기준일. yyyyMMdd. 휴장일 요청 시 Lambda가 직전 거래일로 보정해 내려준다. */
    @JsonProperty("tradeDate")
    private String tradeDate;

    /** 지수 구성종목 수. stocks.size()와 다르면 일부가 errors로 빠진 것이다. */
    @JsonProperty("requestedCount")
    private int requestedCount;

    @JsonProperty("stocks")
    private List<StockPrice> stocks;

    @JsonProperty("errors")
    private List<ErrorItem> errors;

    @Getter
    @NoArgsConstructor
    public static class StockPrice {
        @JsonProperty("stockCode")
        private String stockCode;

        @JsonProperty("stockName")
        private String stockName;

        @JsonProperty("openPrice")
        private BigDecimal openPrice;

        @JsonProperty("closePrice")
        private BigDecimal closePrice;
    }

    @Getter
    @NoArgsConstructor
    public static class ErrorItem {
        @JsonProperty("stockCode")
        private String stockCode;

        @JsonProperty("reason")
        private String reason;
    }
}
