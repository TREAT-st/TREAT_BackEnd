package com.example.demo.domain.stockDailyPrice.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.time.Period;
import java.util.Arrays;
import java.util.Optional;

/**
 * 차트 조회 기간. 모든 기간이 일봉이다.
 *
 *   1W 일봉 약 6개 / 1M 약 22개 / 3M 약 64개 / 1Y 약 250개 / 3Y 약 740개
 */
@Getter
@AllArgsConstructor
public enum ChartRange {
    ONE_WEEK("1W", Period.ofWeeks(1)),
    ONE_MONTH("1M", Period.ofMonths(1)),
    THREE_MONTHS("3M", Period.ofMonths(3)),
    ONE_YEAR("1Y", Period.ofYears(1)),
    THREE_YEARS("3Y", Period.ofYears(3));

    /** 요청 파라미터와 응답에 쓰는 표기. 숫자로 시작해 enum 이름으로는 쓸 수 없다. */
    private final String code;
    private final Period period;

    public static Optional<ChartRange> fromCode(String code) {
        return Arrays.stream(values()).filter(range -> range.code.equals(code)).findFirst();
    }

    /**
     * 기간의 시작일. end에서 period만큼 거슬러 올라간 날이고, 양 끝을 모두 포함한다.
     * 1Y의 end가 2026-10-21이면 2025-10-21 ~ 2026-10-21이다.
     */
    public LocalDate startDate(LocalDate end) {
        return end.minus(period);
    }
}
