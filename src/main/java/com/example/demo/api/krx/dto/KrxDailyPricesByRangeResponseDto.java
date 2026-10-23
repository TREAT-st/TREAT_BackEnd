package com.example.demo.api.krx.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * GetDailyPricesFromKrx 기간 조회 모드({"from","to","tickers"}) 응답. 3년치 백필에 쓴다.
 * 종목별 실패는 errors로 격리되어 오고, 나머지 종목은 그대로 쓸 수 있다.
 */
@Getter
@NoArgsConstructor
public class KrxDailyPricesByRangeResponseDto {

    /** 조회 기간. yyyyMMdd. */
    @JsonProperty("from")
    private String from;

    @JsonProperty("to")
    private String to;

    @JsonProperty("prices")
    private List<StockHistory> prices;

    /** 조회 실패. 서버는 실패 종목으로 친다. */
    @JsonProperty("errors")
    private List<KrxStockIssueDto> errors;

    /** 기간 안에 일봉이 없는 것이 정상인 종목(장기 거래정지, 기간 중 미상장 등). 실패가 아니다. */
    @JsonProperty("skipped")
    private List<KrxStockIssueDto> skipped;

    @Getter
    @NoArgsConstructor
    public static class StockHistory {
        @JsonProperty("stockCode")
        private String stockCode;

        @JsonProperty("rows")
        private List<DailyRow> rows;
    }

    /** 거래정지일처럼 가격이 0인 날은 Lambda가 이미 걸러서 보낸다. */
    @Getter
    @NoArgsConstructor
    public static class DailyRow {
        /** yyyyMMdd */
        @JsonProperty("tradeDate")
        private String tradeDate;

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
