package com.example.demo.api.kis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class KisResponseDto {

    @JsonProperty("rt_cd")
    private String rtCd;

    @JsonProperty("output2")
    private List<DailyData> output2;

    @Getter
    @NoArgsConstructor
    public static class DailyData {

        @JsonProperty("stck_bsop_date")
        private String date;

        @JsonProperty("stck_oprc")
        private String openPrice;

        @JsonProperty("stck_clpr")
        private String closePrice;
    }
}
