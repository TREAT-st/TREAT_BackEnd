package com.example.demo.api.krx.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class KrxOhlcvResponseDto {

    @JsonProperty("effective_trade_date")
    private String effectiveTradeDate;

    @JsonProperty("universe_size")
    private int universeSize;

    @JsonProperty("stocks")
    private List<StockOhlcv> stocks;

    @JsonProperty("errors")
    private List<ErrorItem> errors;

    @Getter
    @NoArgsConstructor
    public static class StockOhlcv {
        @JsonProperty("stock_code")
        private String stockCode;

        @JsonProperty("stock_name")
        private String stockName;

        @JsonProperty("market_cap_rank")
        private int marketCapRank;

        @JsonProperty("close")
        private double[] close;

        @JsonProperty("high")
        private double[] high;

        @JsonProperty("low")
        private double[] low;

        @JsonProperty("volume")
        private double[] volume;
    }

    @Getter
    @NoArgsConstructor
    public static class ErrorItem {
        @JsonProperty("stock_code")
        private String stockCode;

        @JsonProperty("reason")
        private String reason;
    }
}
