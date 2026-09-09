package com.example.demo.api.batch.dto;

import com.example.demo.domain.batch.entity.BatchStatus;
import com.example.demo.domain.batch.entity.BatchStep;
import com.example.demo.domain.batch.entity.DailyBatchOutcome;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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
    public static class DailyBatchResult {
        private LocalDate tradeDate;
        private DailyBatchOutcome outcome;
        private BatchStep failedStep;
        private String message;
    }

    /**
     * 트리거 접수 결과.
     *
     * 중복 트리거도 실패가 아니라 "접수하지 않음"이다. 상태 코드가 아니라 이 필드로 알린다.
     * 자동화 계층이 비-2xx를 실패로 보고 재시도하면 헛돌고,
     * 무엇보다 같은 상황에서 인스턴스 수에 따라 상태 코드가 달라지면 계약이 아니다.
     *
     * accepted는 정확히는 "이 인스턴스의 실행기에 제출됐는가"다.
     * 인스턴스가 여럿이면 다른 서버는 자기 실행기가 한가해 true를 돌려주고,
     * 그 뒤 실행 이력의 IN_PROGRESS를 보고 체인이 멈춘다.
     * 단계가 실제로 돌았는지는 실행 이력을 봐야 한다.
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyBatchTriggerResponse {
        private boolean accepted;
        private String message;
    }

    /** 한 단계의 실행 상태. attempt는 그 단계가 몇 번째 시도인지다. */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchExecutionResponse {
        private BatchStep step;
        private BatchStatus status;
        private int attempt;
        private LocalDateTime startedAt;
        private LocalDateTime finishedAt;
        private String message;
    }

    /** steps는 실행 순서대로다. 아직 시작하지 않은 단계는 목록에 없다. */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchExecutionListResponse {
        private LocalDate tradeDate;
        private List<BatchExecutionResponse> steps;
    }
}
