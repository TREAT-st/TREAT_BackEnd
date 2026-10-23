package com.example.demo.domain.stockDailyPrice.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/** 차트 기간. 모든 기간이 일봉이고, [끝 − 기간, 끝]을 양 끝 포함으로 읽는다. */
class ChartRangeTest {

    private static final LocalDate END = LocalDate.of(2026, 10, 22);

    @Test
    void 기간_코드() {
        assertThat(ChartRange.fromCode("1W")).contains(ChartRange.ONE_WEEK);
        assertThat(ChartRange.fromCode("3Y")).contains(ChartRange.THREE_YEARS);
        assertThat(ChartRange.fromCode("2Y")).isEmpty();
    }

    /** 요구사항 예시: 1년 = 2025.10.22 ~ 2026.10.22 */
    @Test
    void 시작일은_끝_날짜에서_기간만큼_거슬러_올라간_같은_날() {
        assertThat(ChartRange.ONE_WEEK.startDate(END)).isEqualTo(LocalDate.of(2026, 10, 15));
        assertThat(ChartRange.ONE_MONTH.startDate(END)).isEqualTo(LocalDate.of(2026, 9, 22));
        assertThat(ChartRange.THREE_MONTHS.startDate(END)).isEqualTo(LocalDate.of(2026, 7, 22));
        assertThat(ChartRange.ONE_YEAR.startDate(END)).isEqualTo(LocalDate.of(2025, 10, 22));
        assertThat(ChartRange.THREE_YEARS.startDate(END)).isEqualTo(LocalDate.of(2023, 10, 22));
    }

    /** 말일 처리. 3/31의 1개월 전은 2/28(평년)이다. */
    @Test
    void 짧은_달은_그_달의_말일로_맞춘다() {
        assertThat(ChartRange.ONE_MONTH.startDate(LocalDate.of(2026, 3, 31))).isEqualTo(LocalDate.of(2026, 2, 28));
    }
}
