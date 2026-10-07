package com.example.demo.api.volatility.mapper;

import com.example.demo.api.volatility.dto.VolatilityResponseDto.ReportGenerationResult;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 리포트 생성 결과의 status는 클라이언트가 문자열로 비교하는 계약이다.
 * 예전에는 "success", "failure"만 소문자라 PARTIAL_SUCCESS와 표기가 섞여 있었다.
 */
class VolatilityConverterTest {

    private static final LocalDate TRADE_DATE = LocalDate.of(2026, 10, 6);

    @Test
    void 전부_요청되면_SUCCESS() {
        ReportGenerationResult result = VolatilityConverter.toReportGenerationResult(3, List.of(), TRADE_DATE);

        assertThat(result.getStatus()).isEqualTo("SUCCESS");
        assertThat(result.getSuccessCount()).isEqualTo(3);
    }

    @Test
    void 일부만_실패하면_PARTIAL_SUCCESS() {
        ReportGenerationResult result =
                VolatilityConverter.toReportGenerationResult(3, List.of("005930"), TRADE_DATE);

        assertThat(result.getStatus()).isEqualTo("PARTIAL_SUCCESS");
        assertThat(result.getFailedCount()).isEqualTo(1);
    }

    @Test
    void 전부_실패하면_FAILURE() {
        ReportGenerationResult result =
                VolatilityConverter.toReportGenerationResult(2, List.of("005930", "000660"), TRADE_DATE);

        assertThat(result.getStatus()).isEqualTo("FAILURE");
        assertThat(result.getSuccessCount()).isZero();
    }
}
