package com.example.demo.domain.batch.service;

import com.example.demo.common.config.JpaAuditingConfig;
import com.example.demo.domain.batch.entity.BatchExecutionLog;
import com.example.demo.domain.batch.entity.BatchExecutionRef;
import com.example.demo.domain.batch.entity.BatchStartResult;
import com.example.demo.domain.batch.entity.BatchStartDecision;
import com.example.demo.domain.batch.entity.BatchStatus;
import com.example.demo.domain.batch.entity.BatchStep;
import com.example.demo.domain.batch.repository.BatchExecutionLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static com.example.demo.common.consts.StaticVariable.SEOUL_ZONE;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * 배치 실행 이력의 상태 전이 검증.
 *
 * 서비스가 REQUIRES_NEW라 별도 트랜잭션에서 커밋된다. @DataJpaTest의 기본 롤백에 기대면
 * 커밋된 행이 남아 테스트끼리 간섭하므로, 바깥 트랜잭션을 끄고 매 테스트마다 직접 비운다.
 */
@DataJpaTest
@Import({BatchExecutionLogServiceImpl.class, BatchExecutionLogWriter.class, JpaAuditingConfig.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:batchlog;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class BatchExecutionLogServiceImplTest {

    private static final LocalDate TRADE_DATE = LocalDate.of(2026, 8, 14);

    @Autowired
    private BatchExecutionLogService batchExecutionLogService;

    @Autowired
    private BatchExecutionLogRepository batchExecutionLogRepository;

    @BeforeEach
    void clear() {
        batchExecutionLogRepository.deleteAll();
    }

    @Test
    void 기록이_없으면_시작할_수_있다() {
        BatchStartResult result = batchExecutionLogService.tryStart(TRADE_DATE, BatchStep.SYNC, false);

        assertThat(result.decision()).isEqualTo(BatchStartDecision.STARTED);
        assertThat(result.execution().executionId()).isNotNull();
        assertThat(result.execution().attempt()).isEqualTo(1);
        assertThat(log(BatchStep.SYNC).getStatus()).isEqualTo(BatchStatus.RUNNING);
        assertThat(log(BatchStep.SYNC).getStartedAt()).isNotNull();
    }

    /**
     * 이미 성공한 단계는 다시 실행하지 않지만, 체인은 다음 단계로 계속 가야 한다.
     * 그래서 진행 중(IN_PROGRESS)과 반드시 구분된다.
     */
    @Test
    void 이미_성공한_단계는_ALREADY_DONE이다() {
        BatchExecutionRef ref = start(BatchStep.DETECT);
        batchExecutionLogService.succeed(ref, "탐지 10건");

        BatchStartResult result = batchExecutionLogService.tryStart(TRADE_DATE, BatchStep.DETECT, false);

        assertThat(result.decision()).isEqualTo(BatchStartDecision.ALREADY_DONE);
        assertThat(result.execution()).isNull();
        assertThat(log(BatchStep.DETECT).getStatus()).isEqualTo(BatchStatus.SUCCESS);
        assertThat(log(BatchStep.DETECT).getMessage()).isEqualTo("탐지 10건");
    }

    /**
     * 살아 있는 실행이 있으면 체인 전체를 멈춰야 한다.
     * ALREADY_DONE처럼 다음 단계로 넘겨버리면 두 실행이 모두 리포트 생성까지 도달한다.
     */
    @Test
    void 진행_중인_단계는_IN_PROGRESS다() {
        start(BatchStep.SYNC);

        BatchStartResult result = batchExecutionLogService.tryStart(TRADE_DATE, BatchStep.SYNC, false);

        assertThat(result.decision()).isEqualTo(BatchStartDecision.IN_PROGRESS);
        assertThat(result.execution()).isNull();
    }

    @Test
    void 실패한_단계는_재시작할_수_있다() {
        BatchExecutionRef ref = start(BatchStep.REPORT);
        batchExecutionLogService.fail(ref, "Lambda 호출 실패");

        assertThat(batchExecutionLogService.tryStart(TRADE_DATE, BatchStep.REPORT, false).decision())
                .isEqualTo(BatchStartDecision.STARTED);

        BatchExecutionLog restarted = log(BatchStep.REPORT);
        assertThat(restarted.getStatus()).isEqualTo(BatchStatus.RUNNING);
        // 이전 결과가 남아 있으면 지난 실패를 이번 결과로 오인한다.
        assertThat(restarted.getFinishedAt()).isNull();
        assertThat(restarted.getMessage()).isNull();
    }

    /**
     * 체인 도중 앱이 죽으면 RUNNING이 그대로 남는다.
     * 인계 규칙이 없으면 그 거래일은 이후 실행이 영구히 스킵된다.
     */
    @Test
    void 오래된_RUNNING은_죽은_것으로_보고_인계한다() {
        BatchExecutionRef dead = startLongAgo(BatchStep.SYNC);

        BatchStartResult taken = batchExecutionLogService.tryStart(TRADE_DATE, BatchStep.SYNC, false);

        assertThat(taken.decision()).isEqualTo(BatchStartDecision.STARTED);
        assertThat(log(BatchStep.SYNC).getStatus()).isEqualTo(BatchStatus.RUNNING);
        // 같은 행을 재사용하므로 회차로 구분된다.
        assertThat(taken.execution().executionId()).isEqualTo(dead.executionId());
        assertThat(taken.execution().attempt()).isGreaterThan(dead.attempt());
    }

    /**
     * 인계 뒤 이전 실행이 뒤늦게 끝나 결과를 쓰면, 지금 돌고 있는 실행의 상태가 남에 의해 바뀐다.
     * executionId만으로 완료 처리하면 이걸 막을 수 없다.
     */
    @Test
    void 인계된_뒤_도착한_이전_실행의_성공은_무시된다() {
        BatchExecutionRef dead = startLongAgo(BatchStep.DETECT);
        batchExecutionLogService.tryStart(TRADE_DATE, BatchStep.DETECT, false);

        batchExecutionLogService.succeed(dead, "뒤늦게 끝난 이전 실행");

        BatchExecutionLog current = log(BatchStep.DETECT);
        assertThat(current.getStatus()).isEqualTo(BatchStatus.RUNNING);
        assertThat(current.getMessage()).isNull();
    }

    /**
     * 실패 쪽이 더 위험하다. FAILED로 바뀌면 다음 실행이 재시작 대상으로 보고
     * 이미 돌고 있는 실행 위에 하나를 더 띄운다.
     */
    @Test
    void 인계된_뒤_도착한_이전_실행의_실패는_무시된다() {
        BatchExecutionRef dead = startLongAgo(BatchStep.DETECT);
        batchExecutionLogService.tryStart(TRADE_DATE, BatchStep.DETECT, false);

        batchExecutionLogService.fail(dead, "뒤늦게 터진 이전 실행");

        assertThat(log(BatchStep.DETECT).getStatus()).isEqualTo(BatchStatus.RUNNING);
        assertThat(batchExecutionLogService.tryStart(TRADE_DATE, BatchStep.DETECT, false).decision())
                .isEqualTo(BatchStartDecision.IN_PROGRESS);
    }

    /**
     * REPORT는 종목마다 Lambda를 부르느라 정상 소요가 길다.
     * 전 단계에 같은 기준을 쓰면 아직 돌고 있는 REPORT를 재실행이 뺏는다.
     */
    @Test
    void 죽은_실행_판정_기준은_단계마다_다르다() {
        startMinutesAgo(BatchStep.SYNC, 45);      // SYNC 기준 30분 -> 죽은 것으로 본다
        startMinutesAgo(BatchStep.REPORT, 45);    // REPORT 기준 90분 -> 아직 살아 있다

        assertThat(batchExecutionLogService.tryStart(TRADE_DATE, BatchStep.SYNC, false).decision())
                .isEqualTo(BatchStartDecision.STARTED);
        assertThat(batchExecutionLogService.tryStart(TRADE_DATE, BatchStep.REPORT, false).decision())
                .isEqualTo(BatchStartDecision.IN_PROGRESS);
    }

    @Test
    void force면_성공한_단계도_재시작한다() {
        BatchExecutionRef ref = start(BatchStep.DETECT);
        batchExecutionLogService.succeed(ref, "탐지 10건");

        assertThat(batchExecutionLogService.tryStart(TRADE_DATE, BatchStep.DETECT, true).decision())
                .isEqualTo(BatchStartDecision.STARTED);
        assertThat(log(BatchStep.DETECT).getStatus()).isEqualTo(BatchStatus.RUNNING);
    }

    /** 탐지 결과가 0건이면 리포트를 만들 대상이 없다. 실패가 아니라 정상 상황이다. */
    @Test
    void 실행할_대상이_없으면_스킵으로_기록된다() {
        batchExecutionLogService.skip(TRADE_DATE, BatchStep.REPORT, "탐지된 종목 없음");

        BatchExecutionLog skipped = log(BatchStep.REPORT);
        assertThat(skipped.getStatus()).isEqualTo(BatchStatus.SKIPPED);
        assertThat(skipped.getMessage()).isEqualTo("탐지된 종목 없음");
        // 스킵은 실패가 아니지만 재시도 대상이다. 다음 실행에서 다시 판단해야 한다.
        assertThat(batchExecutionLogService.tryStart(TRADE_DATE, BatchStep.REPORT, false).decision())
                .isEqualTo(BatchStartDecision.STARTED);
    }

    /**
     * 살아 있는 실행을 스킵으로 덮으면 그 실행은 계속 도는데 행은 SKIPPED가 된다.
     * 다음 실행이 재시작 대상으로 보고 같은 단계를 하나 더 띄운다.
     */
    @Test
    void 진행_중인_단계는_스킵으로_덮어쓰지_않는다() {
        BatchExecutionRef running = start(BatchStep.REPORT);

        batchExecutionLogService.skip(TRADE_DATE, BatchStep.REPORT, "탐지된 종목 없음");

        assertThat(log(BatchStep.REPORT).getStatus()).isEqualTo(BatchStatus.RUNNING);
        // 그 실행은 여전히 자기 결과를 쓸 수 있어야 한다.
        batchExecutionLogService.succeed(running, "생성 요청 10건");
        assertThat(log(BatchStep.REPORT).getStatus()).isEqualTo(BatchStatus.SUCCESS);
    }

    /** 죽은 RUNNING까지 보호하면 그 거래일이 영영 정리되지 않는다. claim의 인계 규칙과 같은 기준이다. */
    @Test
    void 죽은_RUNNING은_스킵으로_정리된다() {
        startMinutesAgo(BatchStep.REPORT, 240);

        batchExecutionLogService.skip(TRADE_DATE, BatchStep.REPORT, "탐지된 종목 없음");

        assertThat(log(BatchStep.REPORT).getStatus()).isEqualTo(BatchStatus.SKIPPED);
    }

    /**
     * skip이 상태를 무조건 덮어쓰면 성공 이력이 지워진다.
     * 휴장일에 KRX가 직전 거래일을 돌려주므로, 그 날짜로 스킵을 남기면 어제의 성공 3건이 날아간다.
     */
    @Test
    void 이미_성공한_단계는_스킵으로_덮어쓰지_않는다() {
        BatchExecutionRef ref = start(BatchStep.SYNC);
        batchExecutionLogService.succeed(ref, "추가 2, 편출 1");

        batchExecutionLogService.skip(TRADE_DATE, BatchStep.SYNC, "휴장일");

        BatchExecutionLog kept = log(BatchStep.SYNC);
        assertThat(kept.getStatus()).isEqualTo(BatchStatus.SUCCESS);
        assertThat(kept.getMessage()).isEqualTo("추가 2, 편출 1");
    }

    @Test
    void 거래일이_다르면_별개로_기록된다() {
        BatchExecutionRef ref = start(BatchStep.SYNC);
        batchExecutionLogService.succeed(ref, "완료");

        assertThat(batchExecutionLogService.tryStart(LocalDate.of(2026, 8, 18), BatchStep.SYNC, false).decision())
                .isEqualTo(BatchStartDecision.STARTED);
        assertThat(batchExecutionLogRepository.findAll()).hasSize(2);
    }

    /** step이 문자열로 저장돼 DB 정렬은 알파벳순(DETECT, REPORT, SYNC, VERIFY)을 준다. */
    @Test
    void 거래일의_전_단계를_순서대로_조회한다() {
        batchExecutionLogService.tryStart(TRADE_DATE, BatchStep.VERIFY, false);
        batchExecutionLogService.tryStart(TRADE_DATE, BatchStep.REPORT, false);
        batchExecutionLogService.tryStart(TRADE_DATE, BatchStep.SYNC, false);
        batchExecutionLogService.tryStart(TRADE_DATE, BatchStep.DETECT, false);

        assertThat(batchExecutionLogService.getByTradeDate(TRADE_DATE))
                .extracting(BatchExecutionLog::getStep)
                .containsExactly(BatchStep.SYNC, BatchStep.DETECT, BatchStep.REPORT, BatchStep.VERIFY);
    }

    private BatchExecutionRef start(BatchStep step) {
        BatchStartResult result = batchExecutionLogService.tryStart(TRADE_DATE, step, false);
        assertThat(result.decision()).isEqualTo(BatchStartDecision.STARTED);
        return result.execution();
    }

    /**
     * 지정한 시간 전에 시작해 아직 안 끝난 실행을 만든다.
     * 반환하는 ref가 그 실행의 것이라, 인계된 뒤에는 stale이 된다.
     */
    private BatchExecutionRef startMinutesAgo(BatchStep step, int minutes) {
        BatchExecutionRef ref = start(step);

        BatchExecutionLog running = batchExecutionLogRepository.findById(ref.executionId()).orElseThrow();
        running.restart(LocalDateTime.now(SEOUL_ZONE).minusMinutes(minutes));
        batchExecutionLogRepository.saveAndFlush(running);

        return running.ref();
    }

    /** 어느 단계 기준으로도 죽은 것으로 보이는 실행. */
    private BatchExecutionRef startLongAgo(BatchStep step) {
        return startMinutesAgo(step, 240);
    }

    private BatchExecutionLog log(BatchStep step) {
        return batchExecutionLogRepository.findByTradeDateAndStep(TRADE_DATE, step).orElseThrow();
    }
}
