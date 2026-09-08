package com.example.demo.domain.batch.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Duration;

/**
 * 일일 배치의 단계. 순서대로 실행되며 앞 단계가 성공해야 다음이 의미를 갖는다.
 *
 * runningTimeout은 "이 시간이 지나도 RUNNING이면 죽은 실행으로 보고 인계한다"의 기준이다.
 * 단계마다 정상 소요 시간의 상한이 크게 달라 값을 각자 갖는다.
 * 하루 한 번 도는 배치라 넉넉해도 손해가 없다. 오히려 짧으면 살아 있는 실행을 뺏는 쪽이 위험하다.
 */
@Getter
@RequiredArgsConstructor
public enum BatchStep {

    /** KRX 코스피200 Lambda 1회 + DB 반영. 소켓 타임아웃이 6분이라 최악이 그 언저리다. */
    SYNC(Duration.ofMinutes(30)),

    /** KRX OHLCV Lambda 1회 + 지표 계산. 계산은 초 단위라 상한은 SYNC와 같다. */
    DETECT(Duration.ofMinutes(30)),

    /**
     * 종목마다 리포트 Lambda를 부르고, 실패해도 다음 종목으로 넘어간다.
     * 전 종목이 소켓 타임아웃(6분)까지 가면 10종목 × 6분이라 30분으로는 모자란다.
     */
    REPORT(Duration.ofMinutes(90)),

    /**
     * 리포트가 실제로 도착했는지 확인하는 단계. 실행 로직은 Phase 4에서 붙인다.
     *
     * REPORT 성공 직후 시작해 콜백이 다 도착하면 닫힌다. ECS 태스크가 50분쯤 걸리고
     * 그레이스까지 감안하면 정상 대기가 한 시간을 넘는다. 그보다 짧으면 아직 기다리는 중인
     * VERIFY를 재실행이 뺏어 대기 시계가 초기화된다.
     */
    VERIFY(Duration.ofMinutes(180));

    private final Duration runningTimeout;
}
