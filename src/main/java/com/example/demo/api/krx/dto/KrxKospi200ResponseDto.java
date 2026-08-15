package com.example.demo.api.krx.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

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

    /**
     * 구성종목 한 건. 코드·이름은 항상 채워지고, 시세를 못 받은 종목은 가격이 null이다.
     * 시세 실패가 편출을 뜻하지 않으므로 목록에서 빠지지 않는다(사유는 errors에 담긴다).
     */
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

        public boolean hasPrice() {
            return openPrice != null && closePrice != null;
        }
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
