package com.example.demo.api.stockDailyPrice.service;

import com.example.demo.domain.stockDailyPrice.service.StockDailyPriceCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static com.example.demo.common.consts.StaticVariable.SEOUL_ZONE;

/**
 * 일봉 적재(백필·일일)를 서버 전체에서 하나만 돌게 하는 잠금. DB의 임대 잠금(StockDailyPriceJobLock)을 쓴다.
 *
 * 백필·일일 적재: 컨트롤러가 비동기로 넘기기 전에 잡고, 작업이 끝나면 비동기 메서드가 푼다. 그래서 다른 서버에서
 * 돌고 있으면 접수 단계에서 accepted=false로 알릴 수 있다. 실행기 거부처럼 작업이 시작되지 못하면 컨트롤러가 바로 푼다.
 * 삭제: 같은 잠금을 잡은 뒤 동기로 지운다. 적재 중에 지우면 지운 데이터가 다시 들어가거나, 일봉은 없는데
 * 성공 이력이 다시 기록될 수 있기 때문이다.
 *
 * 임대는 고정 시간이 아니라 작업이 단계마다 갱신한다(백필은 묶음마다, 일일 적재는 날짜마다). 작업이 오래 걸려도
 * 임대가 끝나 다른 서버가 끼어들지 않는다. 갱신에 실패하면 다른 실행이 잠금을 가져간 것이므로 작업을 멈춘다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DailyPriceJobGuard {

    /** 백필과 일일 적재가 같은 이름을 쓴다. 둘도 서로 겹치면 안 된다. */
    static final String LOCK_NAME = "STOCK_DAILY_PRICE";

    /**
     * 임대 시간. 작업이 단계마다 갱신하므로, 한 단계(Lambda 한 번 + 저장)보다만 길면 된다.
     * Lambda 타임아웃(5분 권장)보다 넉넉하게 잡는다. 서버가 작업 중에 죽으면 이 시간이 지나야 다시 실행된다.
     */
    static final Duration LEASE = Duration.ofMinutes(15);

    private final StockDailyPriceCommandService stockDailyPriceCommandService;

    /** @return 잡았으면 잠금 토큰. 다른 실행이 갖고 있으면 비어 있다 */
    public Optional<String> tryAcquire() {
        try {
            stockDailyPriceCommandService.createJobLockIfAbsent(LOCK_NAME);
        } catch (DataIntegrityViolationException e) {
            // 다른 서버가 같은 순간에 행을 만들었다. 행은 있으니 그대로 잠금을 시도한다.
        }

        String owner = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now(SEOUL_ZONE);
        boolean acquired = stockDailyPriceCommandService.tryAcquireJobLock(LOCK_NAME, owner, now, now.plus(LEASE));
        if (!acquired) {
            log.info("다른 서버나 실행이 일봉 적재 잠금을 갖고 있어 접수하지 않습니다.");
        }
        return acquired ? Optional.of(owner) : Optional.empty();
    }

    /**
     * 임대를 지금부터 LEASE만큼 연장한다. 긴 작업이 단계를 하나 마칠 때마다 부른다.
     *
     * @return 계속해도 되면 true. false면 잠금을 잃었으니(임대가 끝나 다른 실행이 가져감) 작업을 멈춰야 한다
     */
    public boolean renew(String owner) {
        try {
            boolean renewed = stockDailyPriceCommandService.extendJobLock(
                    LOCK_NAME, owner, LocalDateTime.now(SEOUL_ZONE).plus(LEASE));
            if (!renewed) {
                log.error("일봉 적재 잠금을 잃었습니다. 다른 실행과 겹치지 않도록 작업을 멈춥니다. owner={}", owner);
            }
            return renewed;
        } catch (Exception e) {
            // 갱신 자체가 실패했으면(DB 일시 장애 등) 잠금을 아직 갖고 있을 수 있다. 계속하고 다음 단계에서 다시 갱신한다.
            log.warn("일봉 적재 잠금 갱신 실패. 다음 단계에서 다시 시도합니다. owner={}", owner, e);
            return true;
        }
    }

    /** 잠금을 푼다. 실패해도 임대 시간이 지나면 풀리므로 예외를 밖으로 내지 않는다. */
    public void release(String owner) {
        try {
            stockDailyPriceCommandService.releaseJobLock(LOCK_NAME, owner);
        } catch (Exception e) {
            log.error("일봉 적재 잠금 해제 실패. 임대가 끝나면 풀립니다. owner={}", owner, e);
        }
    }
}
