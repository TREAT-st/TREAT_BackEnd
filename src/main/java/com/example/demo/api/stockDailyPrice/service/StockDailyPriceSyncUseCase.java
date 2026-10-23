package com.example.demo.api.stockDailyPrice.service;

import com.example.demo.api.krx.dto.KrxDailyPricesByDateResponseDto;
import com.example.demo.api.krx.service.KrxService;
import com.example.demo.api.stockDailyPrice.service.DailyPriceIngestor.Ingested;
import com.example.demo.api.stockDailyPrice.service.DailyPriceIngestor.RawRow;
import com.example.demo.common.annotation.UseCase;
import com.example.demo.domain.stock.service.StockQueryService;
import com.example.demo.domain.stockDailyPrice.entity.DailyPriceSyncResult;
import com.example.demo.domain.stockDailyPrice.entity.DailyPriceSyncStatus;
import com.example.demo.domain.stockDailyPrice.entity.StockDailyPriceSyncLog;
import com.example.demo.domain.stockDailyPrice.exception.StockDailyPriceHandler;
import com.example.demo.domain.stockDailyPrice.service.StockDailyPriceCommandService;
import com.example.demo.domain.stockDailyPrice.service.StockDailyPriceQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Async;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import static com.example.demo.common.consts.StaticVariable.SEOUL_ZONE;
import static com.example.demo.common.consts.StaticVariable.DAILY_PRICE_SYNC_FAILED_COUNT;
import static com.example.demo.common.consts.StaticVariable.DAILY_PRICE_SYNC_HOLIDAY;
import static com.example.demo.common.consts.StaticVariable.DAILY_PRICE_SYNC_LOOKBACK_DAYS;
import static com.example.demo.common.consts.StaticVariable.DAILY_PRICE_SYNC_MAX_ATTEMPT;

/**
 * 일봉 일일 적재(/daily). 매일 스케줄러가 호출한다.
 *
 * 날짜 하나를 시장 전체 조회 한 번으로 받는다(Lambda 1회, KRX 요청 1회). 종목마다 조회하면 매일 200번이다.
 *
 * 무엇을 받을지는 거래일별 적재 이력(StockDailyPriceSyncLog)으로 정한다. 일봉 테이블의 최신 날짜만 보면
 * 199종목이 저장되고 한 종목이 실패한 날을 다시 요청하지 못한다. 이력에는 날짜별 상태와 실패 종목이 남아,
 * 다음 실행이 실패 종목만 다시 받는다. 실행기가 바빠 거부된 날, 호출이 실패한 날도 다음 실행이 따라잡는다.
 *
 * 백필과 같은 실행기(dailyPriceTaskExecutor)를 쓰고, 서버가 여러 대여도 DB 잠금(DailyPriceJobGuard)으로
 * 한 번에 하나만 돈다.
 * 클래스 레벨 @Transactional을 붙이지 않는다. 날짜마다 저장과 이력 기록이 각자의 트랜잭션으로 끝난다.
 */
@Slf4j
@UseCase
@RequiredArgsConstructor
public class StockDailyPriceSyncUseCase {

    private final KrxService krxService;
    private final StockQueryService stockQueryService;
    private final StockDailyPriceQueryService stockDailyPriceQueryService;
    private final StockDailyPriceCommandService stockDailyPriceCommandService;
    private final DailyPriceIngestor dailyPriceIngestor;
    private final DailyPriceJobGuard dailyPriceJobGuard;

    /**
     * 받을 날짜와 종목을 정한다. 비동기로 넘기기 전에 불러야 한다.
     *
     * date를 지정하면 그 날짜만 전 종목으로 다시 받는다. 최근 거래일보다 뒤면 400(4255).
     *
     * date를 생략하면:
     *   최근 거래일 L = stock에 반영된 가장 최근 거래일 (없으면 409, 4256)
     *   시작점 S = (끝난 마지막 이력 + 1)과 (L − 30일) 중 이른 날
     *              이력이 없으면 일봉 테이블의 마지막 날짜 + 1 (백필 직후 첫 실행)
     *              일봉도 없으면 L (과거는 /backfill로 받는다)
     *   대상 = [S, L]의 평일 중 이력이 없는 날 → 전 종목
     *          최근 30일 안의 빈 날(이력 기록 실패, 앞 날짜를 먼저 처리한 경우, 전 종목 삭제)도 여기서 다시 찾는다.
     *          장애가 30일보다 길었으면 끝난 마지막 이력 다음 날부터 찾는다
     *        + 시도 횟수가 상한 미만인 PARTIAL·FAILED 날 → 기록된 실패 종목(없으면 전 종목)
     * 주말은 처음부터 뺀다. 평일 휴장일은 Lambda가 알려주어 HOLIDAY로 기록된다.
     */
    public DailyPlan plan(LocalDate date) {
        LocalDate latest = stockQueryService.getLatestTradeDate()
                .orElseThrow(StockDailyPriceHandler::priceNotSyncedYet);
        List<String> allCodes = stockQueryService.getAllStockCodes();

        if (date != null) {
            if (date.isAfter(latest)) {
                throw StockDailyPriceHandler.invalidRange();
            }
            List<DailyTarget> targets = allCodes.isEmpty() ? List.of() : List.of(new DailyTarget(date, allCodes));
            return new DailyPlan(latest, targets);
        }

        Map<LocalDate, List<String>> byDate = new TreeMap<>();

        for (StockDailyPriceSyncLog retry : stockDailyPriceQueryService.getRetryableSyncLogs(DAILY_PRICE_SYNC_MAX_ATTEMPT)) {
            if (retry.getTradeDate().isAfter(latest)) {
                continue;
            }
            List<String> failed = retry.failedStockCodeList();
            byDate.put(retry.getTradeDate(), failed.isEmpty() ? allCodes : failed);
        }

        LocalDate start = startDate(latest);
        if (!start.isAfter(latest)) {
            Map<LocalDate, StockDailyPriceSyncLog> logs = stockDailyPriceQueryService.getSyncLogs(start, latest);
            for (LocalDate d = start; !d.isAfter(latest); d = d.plusDays(1)) {
                if (!isWeekend(d) && !logs.containsKey(d)) {
                    byDate.put(d, allCodes);
                }
            }
        }

        List<DailyTarget> targets = byDate.entrySet().stream()
                .filter(e -> !e.getValue().isEmpty())
                .map(e -> new DailyTarget(e.getKey(), e.getValue()))
                .toList();
        return new DailyPlan(latest, targets);
    }

    private LocalDate startDate(LocalDate latest) {
        Optional<LocalDate> earliestLog = stockDailyPriceQueryService.getEarliestSyncDate();
        if (earliestLog.isEmpty()) {
            return stockDailyPriceQueryService.getLatestDailyPriceDateOverall()
                    .map(d -> d.plusDays(1))
                    .orElse(latest);
        }

        // 첫 이력보다 앞이라도 최근 30일 안이면 본다. 전 종목 삭제로 앞쪽 이력이 지워져도 그 날짜를 다시 찾는다.
        // 운영 첫 달에는 백필로 채운 날도 이력이 없어 한 번씩 다시 받지만, upsert라 값만 갱신되고 그 뒤로는 이력이 남는다.
        LocalDate afterFinished = stockDailyPriceQueryService.getLatestFinishedSyncDate()
                .map(d -> d.plusDays(1))
                .orElse(earliestLog.get());
        LocalDate lookback = latest.minusDays(DAILY_PRICE_SYNC_LOOKBACK_DAYS);
        return afterFinished.isBefore(lookback) ? afterFinished : lookback;
    }

    private static boolean isWeekend(LocalDate date) {
        return date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY;
    }

    /** @param lockToken 컨트롤러가 잡은 일봉 적재 잠금. 끝나면 푼다 */
    @Async("dailyPriceTaskExecutor")
    public void runAsync(DailyPlan plan, String lockToken) {
        try {
            List<DateResult> results = run(plan, lockToken);
            log.info("일봉 일일 적재 종료. 최근거래일={} 날짜={}", plan.latestTradeDate(),
                    results.stream().map(r -> r.tradeDate() + ":" + r.status()).toList());
        } finally {
            dailyPriceJobGuard.release(lockToken);
        }
    }

    /**
     * 날짜마다 받고, 저장하고, 이력을 남긴다. 한 날짜의 실패는 그 날짜만 FAILED로 남기고 다음 날짜로 넘어간다.
     *
     * 날짜를 마칠 때마다 잠금 임대를 갱신한다. 잠금을 잃었으면 남은 날짜는 처리하지 않는다.
     * 이력이 남지 않으니 다음 실행이 그 날짜들을 다시 찾는다.
     *
     * @param lockToken 일봉 적재 잠금. null이면 갱신하지 않는다(테스트 등 잠금 없이 직접 부를 때)
     */
    public List<DateResult> run(DailyPlan plan, String lockToken) {
        List<DateResult> results = new ArrayList<>();
        List<DailyTarget> targets = plan.targets();
        for (int i = 0; i < targets.size(); i++) {
            DailyTarget target = targets.get(i);
            DailyPriceSyncResult result = syncDate(target);
            record(target.tradeDate(), result);
            results.add(new DateResult(target.tradeDate(), result.status(), result.failedStockCodes()));

            boolean hasNext = i + 1 < targets.size();
            if (hasNext && lockToken != null && !dailyPriceJobGuard.renew(lockToken)) {
                log.error("일봉 일일 적재를 중단합니다. 처리하지 못한 날짜={}",
                        targets.subList(i + 1, targets.size()).stream().map(DailyTarget::tradeDate).toList());
                break;
            }
        }
        return results;
    }

    /**
     * 상태 판정:
     *   marketClosed → HOLIDAY
     *   실패 종목 없음 → SUCCESS (skipped는 실패가 아니다)
     *   받은 것도 건너뛴 것도 없이 전부 실패 → FAILED
     *   그 외 → PARTIAL
     *   호출·저장 예외 → FAILED (요청 종목 전체를 실패 종목으로)
     */
    private DailyPriceSyncResult syncDate(DailyTarget target) {
        LocalDateTime startedAt = LocalDateTime.now(SEOUL_ZONE);
        LocalDate date = target.tradeDate();
        List<String> requested = target.stockCodes();

        try {
            KrxDailyPricesByDateResponseDto response = krxService.getDailyPricesByDate(date, requested);
            if (response.isMarketClosed()) {
                return result(DailyPriceSyncStatus.HOLIDAY, requested.size(), 0, 0, List.of(), startedAt, DAILY_PRICE_SYNC_HOLIDAY);
            }

            // 날짜 모드에는 errors가 없다. 시장 전체를 한 번에 조회해서, 실패하면 Lambda가 예외로 알린다.
            Ingested ingested = dailyPriceIngestor.ingest(
                    rawRows(response, date), response.getSkipped(), List.of(), requested, date, date);
            int upserted = dailyPriceIngestor.saveWithOneRetry(ingested.rows(), date);
            List<String> failed = ingested.failed();
            List<String> skipped = ingested.skipped();

            DailyPriceSyncStatus status;
            if (failed.isEmpty()) {
                status = DailyPriceSyncStatus.SUCCESS;
            } else if (ingested.rows().isEmpty() && skipped.isEmpty()) {
                status = DailyPriceSyncStatus.FAILED;
            } else {
                status = DailyPriceSyncStatus.PARTIAL;
            }
            String message = failed.isEmpty() ? null : DAILY_PRICE_SYNC_FAILED_COUNT.formatted(failed.size());
            return result(status, requested.size(), upserted, skipped.size(), failed, startedAt, message);
        } catch (Exception e) {
            log.error("일봉 일일 적재 실패. date={} 종목={}건", date, requested.size(), e);
            return result(DailyPriceSyncStatus.FAILED, requested.size(), 0, 0, requested, startedAt,
                    e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    /**
     * 이력 기록이 실패해도 다음 날짜는 계속 처리한다. 기록이 없는 날은 최근 30일 안이면
     * 다음 실행이 다시 찾는다(DAILY_PRICE_SYNC_LOOKBACK_DAYS). 데이터는 이미 저장됐어도 다시 받으면 갱신만 일어난다.
     */
    private void record(LocalDate date, DailyPriceSyncResult result) {
        try {
            try {
                stockDailyPriceCommandService.recordSyncResult(date, result);
            } catch (DataIntegrityViolationException e) {
                // 같은 날짜의 이력을 다른 실행이 먼저 만들었다. 다시 시도하면 그 행을 찾아 덮어쓴다.
                stockDailyPriceCommandService.recordSyncResult(date, result);
            }
        } catch (Exception e) {
            log.error("일봉 적재 이력 기록 실패. date={} status={}", date, result.status(), e);
        }
    }

    private static DailyPriceSyncResult result(DailyPriceSyncStatus status, int requested, int upserted, int skipped,
                                               List<String> failed, LocalDateTime startedAt, String message) {
        return new DailyPriceSyncResult(status, requested, upserted, skipped, failed,
                startedAt, LocalDateTime.now(SEOUL_ZONE), message);
    }

    /** 날짜 모드 응답은 행에 거래일이 없다. 응답 날짜가 요청과 같은지는 KrxService가 확인했으므로 요청 날짜를 붙인다. */
    private static List<RawRow> rawRows(KrxDailyPricesByDateResponseDto response, LocalDate date) {
        if (response.getPrices() == null) {
            return List.of();
        }
        String rawDate = response.getDate();
        return response.getPrices().stream()
                .map(p -> new RawRow(p.getStockCode(), date, rawDate,
                        p.getOpenPrice(), p.getHighPrice(), p.getLowPrice(), p.getClosePrice()))
                .toList();
    }

    public record DailyTarget(LocalDate tradeDate, List<String> stockCodes) {
    }

    /** @param targets 거래일 오름차순. 비어 있으면 받을 것이 없다(이미 최신) */
    public record DailyPlan(LocalDate latestTradeDate, List<DailyTarget> targets) {

        public boolean isEmpty() {
            return targets.isEmpty();
        }

        public List<LocalDate> tradeDates() {
            return targets.stream().map(DailyTarget::tradeDate).toList();
        }
    }

    public record DateResult(LocalDate tradeDate, DailyPriceSyncStatus status, List<String> failedStockCodes) {
    }
}
