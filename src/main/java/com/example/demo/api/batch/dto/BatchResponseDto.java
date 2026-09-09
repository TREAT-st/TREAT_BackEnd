package com.example.demo.api.batch.dto;

import com.example.demo.domain.batch.entity.BatchStep;
import com.example.demo.domain.batch.entity.DailyBatchOutcome;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

public class BatchResponseDto {

    /**
     * 체인 실행 결과.
     *
     * 예외를 밖으로 던지지 않고 여기에 담는다. 체인은 비동기로 돌 예정이라 던져도 받을 곳이 없고,
     * 로그와 DB만 봐서는 어느 단계에서 왜 멈췄는지 즉시 판단하기 어렵다.
     *
     * failedStep은 ABORTED일 때만 값이 있다.
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VolatilityDailyBatchResult {
        private LocalDate tradeDate;
        private DailyBatchOutcome outcome;
        private BatchStep failedStep;
        private String message;
    }
}
