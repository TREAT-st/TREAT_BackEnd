package com.example.demo.domain.batch.repository;

import com.example.demo.common.config.JpaAuditingConfig;
import com.example.demo.domain.batch.entity.BatchExecutionLog;
import com.example.demo.domain.batch.entity.BatchStatus;
import com.example.demo.domain.batch.entity.BatchStep;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 스위퍼가 쓰는 조회 조건 검증.
 *
 * 스위퍼 단위 테스트는 이 리포지토리를 목으로 대체하므로, 그레이스 경계와 필터가
 * 실제로 동작하는지는 여기서만 확인된다. 조건이 하나라도 빠지면 엉뚱한 단계를 끝내버린다.
 */
@DataJpaTest
@Import(JpaAuditingConfig.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:batchlogrepo;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class BatchExecutionLogRepositoryTest {

    /** 스위퍼가 "지금 - 80분"으로 계산해 넘기는 값에 해당한다. */
    private static final LocalDateTime DEADLINE = LocalDateTime.of(2026, 9, 9, 17, 28);
    private static final LocalDate TRADE_DATE = LocalDate.of(2026, 9, 9);

    @Autowired
    private BatchExecutionLogRepository batchExecutionLogRepository;

    @BeforeEach
    void clear() {
        batchExecutionLogRepository.deleteAll();
    }

    @Test
    void 그레이스를_넘긴_도착_확인만_대상이다() {
        save(TRADE_DATE, BatchStep.VERIFY, BatchStatus.RUNNING, DEADLINE.minusMinutes(1));

        assertThat(stale()).singleElement()
                .satisfies(found -> assertThat(found.getTradeDate()).isEqualTo(TRADE_DATE));
    }

    /** 아직 리포트를 기다리는 중이다. 여기서 끝내면 정상 도착을 미도착으로 판정한다. */
    @Test
    void 그레이스_전에_시작한_도착_확인은_대상이_아니다() {
        save(TRADE_DATE, BatchStep.VERIFY, BatchStatus.RUNNING, DEADLINE.plusMinutes(1));

        assertThat(stale()).isEmpty();
    }

    /** 다른 단계의 RUNNING까지 끌어오면 돌고 있는 탐지나 리포트 요청을 끝내버린다. */
    @Test
    void 다른_단계의_RUNNING은_대상이_아니다() {
        save(TRADE_DATE, BatchStep.SYNC, BatchStatus.RUNNING, DEADLINE.minusHours(3));
        save(TRADE_DATE, BatchStep.DETECT, BatchStatus.RUNNING, DEADLINE.minusHours(3));
        save(TRADE_DATE, BatchStep.REPORT, BatchStatus.RUNNING, DEADLINE.minusHours(3));

        assertThat(stale()).isEmpty();
    }

    @Test
    void 이미_끝난_도착_확인은_대상이_아니다() {
        save(LocalDate.of(2026, 9, 7), BatchStep.VERIFY, BatchStatus.SUCCESS, DEADLINE.minusDays(2));
        save(LocalDate.of(2026, 9, 8), BatchStep.VERIFY, BatchStatus.FAILED, DEADLINE.minusDays(1));
        save(TRADE_DATE, BatchStep.VERIFY, BatchStatus.SKIPPED, DEADLINE.minusHours(3));

        assertThat(stale()).isEmpty();
    }

    /** 여러 거래일이 밀려 있으면 전부 가져와야 한다. 하루라도 남으면 영영 RUNNING이다. */
    @Test
    void 밀려_있는_거래일을_모두_가져온다() {
        save(LocalDate.of(2026, 9, 7), BatchStep.VERIFY, BatchStatus.RUNNING, DEADLINE.minusDays(2));
        save(LocalDate.of(2026, 9, 8), BatchStep.VERIFY, BatchStatus.RUNNING, DEADLINE.minusDays(1));

        assertThat(stale()).hasSize(2);
    }

    private java.util.List<BatchExecutionLog> stale() {
        return batchExecutionLogRepository.findAllByStepAndStatusAndStartedAtBefore(
                BatchStep.VERIFY, BatchStatus.RUNNING, DEADLINE);
    }

    private void save(LocalDate tradeDate, BatchStep step, BatchStatus status, LocalDateTime startedAt) {
        batchExecutionLogRepository.saveAndFlush(BatchExecutionLog.builder()
                .tradeDate(tradeDate)
                .step(step)
                .status(status)
                .attempt(1)
                .startedAt(startedAt)
                .build());
    }
}
