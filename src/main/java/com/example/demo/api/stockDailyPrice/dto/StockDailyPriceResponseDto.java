package com.example.demo.api.stockDailyPrice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;


public class StockDailyPriceResponseDto {

    /**
     * 백필 접수 결과. 접수되지 않아도 202다(일일 배치 트리거와 같은 계약).
     * from·to·targetStockCount는 서버가 확정한 대상이다. 파라미터를 비우면 기본값이 채워진다.
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BackfillTriggerResponse {
        private boolean accepted;
        private String message;
        private LocalDate from;
        private LocalDate to;
        private int targetStockCount;
    }

    /**
     * 일일 적재 접수 결과. 접수되지 않아도 202다.
     * tradeDates는 이번에 받을 거래일(오름차순)이다. 비어 있으면 이미 최신이라 아무것도 실행하지 않았다(accepted=true).
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyTriggerResponse {
        private boolean accepted;
        private String message;
        /** stock에 반영된 가장 최근 거래일. 받을 수 있는 마지막 날짜다. */
        private LocalDate latestTradeDate;
        private List<LocalDate> tradeDates;
    }

    /** 일봉 삭제 결과. 지운 데이터는 자동으로 다시 채워지지 않는다(message 참고). */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeleteResponse {
        private LocalDate from;
        private LocalDate to;
        /** null이면 전 종목 */
        private String stockCode;
        private int deletedRows;
        /** 함께 지운 일일 적재 이력 수. 한 종목 삭제는 이력을 지우지 않아 0이다. */
        private int deletedSyncLogs;
        private String message;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StockChartResponse {
        private String stockCode;
        private String stockName;
        /** 요청한 기간. 1W, 1M, 3M, 1Y, 3Y */
        private String range;
        /** 봉 단위. 모든 기간이 일봉이라 항상 day */
        private String interval;
        /** 거래일 오름차순. 아직 일봉이 쌓이지 않았으면 빈 배열. */
        private List<ChartCandleResponse> data;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChartCandleResponse {
        /** 거래일 */
        private LocalDate tradeDate;
        private BigDecimal openPrice;
        /** 고가·저가를 받지 못한 날은 null. */
        private BigDecimal highPrice;
        private BigDecimal lowPrice;
        private BigDecimal closePrice;
    }
}
