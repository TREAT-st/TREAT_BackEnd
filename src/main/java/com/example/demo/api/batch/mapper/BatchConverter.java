package com.example.demo.api.batch.mapper;

import com.example.demo.api.batch.dto.BatchResponseDto.BatchExecutionListResponse;
import com.example.demo.api.batch.dto.BatchResponseDto.DailyBatchTriggerResponse;
import com.example.demo.api.batch.dto.BatchResponseDto.BatchExecutionResponse;
import com.example.demo.api.batch.dto.BatchResponseDto.DailyBatchResult;
import com.example.demo.domain.batch.entity.BatchExecutionLog;
import com.example.demo.domain.batch.entity.BatchStep;
import com.example.demo.domain.batch.entity.DailyBatchOutcome;

import java.time.LocalDate;
import java.util.List;

import static com.example.demo.common.consts.StaticVariable.BATCH_ACCEPTED;
import static com.example.demo.common.consts.StaticVariable.BATCH_NOT_ACCEPTED_ALREADY_RUNNING;
import static com.example.demo.common.consts.StaticVariable.NOT_A_TRADING_DAY;

public class BatchConverter {

    public static DailyBatchResult toHolidayResult(LocalDate tradeDate) {
        return DailyBatchResult.builder()
                .tradeDate(tradeDate)
                .outcome(DailyBatchOutcome.HOLIDAY)
                .message(NOT_A_TRADING_DAY)
                .build();
    }

    public static DailyBatchResult toCompletedResult(LocalDate tradeDate) {
        return DailyBatchResult.builder()
                .tradeDate(tradeDate)
                .outcome(DailyBatchOutcome.COMPLETED)
                .build();
    }

    public static DailyBatchResult toAbortedResult(LocalDate tradeDate, BatchStep failedStep,
                                                             String message) {
        return DailyBatchResult.builder()
                .tradeDate(tradeDate)
                .outcome(DailyBatchOutcome.ABORTED)
                .failedStep(failedStep)
                .message(message)
                .build();
    }

    public static DailyBatchTriggerResponse toAcceptedResponse() {
        return DailyBatchTriggerResponse.builder().accepted(true).message(BATCH_ACCEPTED).build();
    }

    public static DailyBatchTriggerResponse toAlreadyRunningResponse() {
        return DailyBatchTriggerResponse.builder()
                .accepted(false)
                .message(BATCH_NOT_ACCEPTED_ALREADY_RUNNING)
                .build();
    }

    /** 실행 이력 엔티티를 그대로 노출하지 않는다. 정렬은 조회 서비스가 이미 실행 순서로 맞춰준다. */
    public static BatchExecutionListResponse toBatchExecutionListResponse(
            LocalDate tradeDate, List<BatchExecutionLog> executions) {
        return BatchExecutionListResponse.builder()
                .tradeDate(tradeDate)
                .steps(executions.stream().map(BatchConverter::toBatchExecutionResponse).toList())
                .build();
    }

    private static BatchExecutionResponse toBatchExecutionResponse(BatchExecutionLog execution) {
        return BatchExecutionResponse.builder()
                .step(execution.getStep())
                .status(execution.getStatus())
                .attempt(execution.getAttempt())
                .startedAt(execution.getStartedAt())
                .finishedAt(execution.getFinishedAt())
                .message(execution.getMessage())
                .build();
    }
}
