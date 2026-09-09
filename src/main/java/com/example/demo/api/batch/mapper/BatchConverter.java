package com.example.demo.api.batch.mapper;

import com.example.demo.api.batch.dto.BatchResponseDto.VolatilityDailyBatchResult;
import com.example.demo.domain.batch.entity.BatchStep;
import com.example.demo.domain.batch.entity.DailyBatchOutcome;

import java.time.LocalDate;

import static com.example.demo.common.consts.StaticVariable.NOT_A_TRADING_DAY;

public class BatchConverter {

    public static VolatilityDailyBatchResult toHolidayResult(LocalDate tradeDate) {
        return VolatilityDailyBatchResult.builder()
                .tradeDate(tradeDate)
                .outcome(DailyBatchOutcome.HOLIDAY)
                .message(NOT_A_TRADING_DAY)
                .build();
    }

    public static VolatilityDailyBatchResult toCompletedResult(LocalDate tradeDate) {
        return VolatilityDailyBatchResult.builder()
                .tradeDate(tradeDate)
                .outcome(DailyBatchOutcome.COMPLETED)
                .build();
    }

    public static VolatilityDailyBatchResult toAbortedResult(LocalDate tradeDate, BatchStep failedStep,
                                                             String message) {
        return VolatilityDailyBatchResult.builder()
                .tradeDate(tradeDate)
                .outcome(DailyBatchOutcome.ABORTED)
                .failedStep(failedStep)
                .message(message)
                .build();
    }
}
