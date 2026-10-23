package com.example.demo.api.krx.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 일봉 Lambda(GetDailyPricesFromKrx) 응답의 종목별 사유. skipped(정상적인 무가격)와 errors(조회 실패)에 쓴다. */
@Getter
@NoArgsConstructor
public class KrxStockIssueDto {

    @JsonProperty("stockCode")
    private String stockCode;

    @JsonProperty("reason")
    private String reason;
}
