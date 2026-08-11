package com.example.demo.api.krx.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * KRX(pykrx) 기반 시총 상위 OHLCV 조회 Lambda의 응답 계약.
 * 유효 거래일 20일 미만 등으로 제외된 종목은 stocks에서 빠지고 errors에 사유가 담긴다(종목별 실패 격리).
 * Lambda가 이미 정제(0값 행 제거)·트리밍(최근 60거래일)까지 마친 배열을 내려주므로
 * Java 쪽은 별도 가공 없이 바로 지표 계산에 사용한다.
 */
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
