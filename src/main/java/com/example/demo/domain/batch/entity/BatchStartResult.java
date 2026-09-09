package com.example.demo.domain.batch.entity;

/**
 * ALREADY_DONE과 IN_PROGRESS를 한 값으로 뭉뚱그리면 안 된다.
 * 전자는 다음 단계로 넘어가야 하고, 후자는 체인을 멈춰야 한다.
 * 둘을 구분하지 않으면 재시도가 겹쳤을 때 두 실행이 모두 리포트 생성까지 도달해 비용이 두 배로 나간다.
 *
 * @param execution STARTED일 때만 값이 있다. succeed/fail에 그대로 넘긴다.
 */
public record BatchStartResult(BatchStartDecision decision, BatchExecutionRef execution) {

    public static BatchStartResult started(BatchExecutionRef ref) {
        return new BatchStartResult(BatchStartDecision.STARTED, ref);
    }

    public static BatchStartResult alreadyDone() {
        return new BatchStartResult(BatchStartDecision.ALREADY_DONE, null);
    }

    public static BatchStartResult inProgress() {
        return new BatchStartResult(BatchStartDecision.IN_PROGRESS, null);
    }
}
