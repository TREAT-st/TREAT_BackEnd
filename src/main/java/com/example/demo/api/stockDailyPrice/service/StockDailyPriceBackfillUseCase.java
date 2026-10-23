package com.example.demo.api.stockDailyPrice.service;

import com.example.demo.api.krx.dto.KrxDailyPricesByRangeResponseDto;
import com.example.demo.api.krx.service.KrxService;
import com.example.demo.api.stockDailyPrice.service.DailyPriceIngestor.Ingested;
import com.example.demo.api.stockDailyPrice.service.DailyPriceIngestor.RawRow;
import com.example.demo.common.annotation.UseCase;
import com.example.demo.domain.stock.service.StockQueryService;
import com.example.demo.domain.stockDailyPrice.exception.StockDailyPriceHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 3년치 일봉 백필(/backfill). 처음 한 번, 새로 편입된 종목, 실패 종목 재시도, 삭제 후 다시 채우기에 쓴다.
 * 매일 하루치는 일일 적재(StockDailyPriceSyncUseCase)가 맡는다.
 *
 * 클래스 레벨 @Transactional을 붙이지 않는다. 묶음마다 수십 초짜리 Lambda 호출이 끼므로
 * 그동안 DB 커넥션을 붙잡으면 안 된다. 저장은 묶음마다 일봉 커맨드 서비스의 트랜잭션으로 끝낸다.
 * 그래서 중간 묶음이 실패해도 앞서 저장한 묶음은 남고, 다시 실행하면 upsert라 이어서 채워진다.
 *
 * 일일 적재 이력(StockDailyPriceSyncLog)은 쓰지 않는다. 백필 결과는 로그("일봉 백필 종료")로 남고,
 * 실패 종목은 stockCodes로 다시 백필한다.
 *
 * 일일 배치와 다른 실행기(dailyPriceTaskExecutor)를 써서 백필이 일일 배치를 막지 않는다.
 * 백필과 일일 적재는 서버가 여러 대여도 한 번에 하나만 돈다(DailyPriceJobGuard의 DB 잠금).
 * 잠금이 임대 시간을 넘겨 겹치는 드문 경우의 유니크 충돌은 묶음 저장을 한 번 다시 시도해 흡수한다.
 */
@Slf4j
@UseCase
@RequiredArgsConstructor
public class StockDailyPriceBackfillUseCase {

    /**
     * 한 번에 Lambda로 보내는 종목 수. 동기 호출 응답 상한이 6MB라 나눠서 보낸다.
     * 3년치 20종목이면 약 1.7MB다. Lambda 쪽 상한(50종목)보다 여유 있게 잡았다.
     */
    static final int CHUNK_SIZE = 20;

    /** 기본 백필 기간. 3Y 차트([끝 − 3년, 끝])를 채운다. */
    private static final int DEFAULT_YEARS = 3;

    private static final String STOCK_CODE_PATTERN = "\\d{6}";

    private final KrxService krxService;
    private final StockQueryService stockQueryService;
    private final DailyPriceIngestor dailyPriceIngestor;
    private final DailyPriceJobGuard dailyPriceJobGuard;

    /**
     * 받을 대상을 정한다. 비동기로 넘기기 전에 불러야 한다.
     * 비동기 안에서 검증하면 잘못된 요청이 202로 접수되고 오류는 로그에만 남는다.
     *
     * 기간은 양 끝 포함이다. to를 생략하면 시세가 반영된 가장 최근 거래일, from을 생략하면 to로부터 3년 전이다.
     *   - to > 최근 거래일, from > to → 400(4255). 최근 거래일보다 새로운 날짜는 확정된 일봉이 없다
     *   - 반영된 시세가 아직 없음 → 409(4256)
     * stockCodes를 생략하면 stock의 전 종목(편출 포함)이다. 지정하면 중복을 없애고 요청 순서를 지킨다.
     * 6자리 숫자가 아니거나 stock에 없는 코드가 하나라도 있으면 요청 전체를 거절한다(400, 4257).
     */
    public BackfillPlan plan(List<String> stockCodes, LocalDate from, LocalDate to) {
        LocalDate latest = stockQueryService.getLatestTradeDate()
                .orElseThrow(StockDailyPriceHandler::priceNotSyncedYet);

        LocalDate resolvedTo = to != null ? to : latest;
        LocalDate resolvedFrom = from != null ? from : resolvedTo.minusYears(DEFAULT_YEARS);
        if (resolvedTo.isAfter(latest) || resolvedFrom.isAfter(resolvedTo)) {
            throw StockDailyPriceHandler.invalidRange();
        }

        return new BackfillPlan(resolvedFrom, resolvedTo, resolveStockCodes(stockCodes));
    }

    private List<String> resolveStockCodes(List<String> requested) {
        List<String> all = stockQueryService.getAllStockCodes();
        if (requested == null || requested.isEmpty()) {
            return all;
        }

        List<String> codes = List.copyOf(new LinkedHashSet<>(
                requested.stream().map(c -> c == null ? "" : c.trim()).toList()));
        Set<String> known = Set.copyOf(all);
        List<String> unknown = codes.stream()
                .filter(code -> !code.matches(STOCK_CODE_PATTERN) || !known.contains(code))
                .toList();
        if (!unknown.isEmpty()) {
            log.warn("일봉 백필 요청에 형식이 틀리거나 stock에 없는 종목코드가 있어 거절합니다. codes={}", unknown);
            throw StockDailyPriceHandler.unknownStockCodes();
        }
        return codes;
    }

    /** @param lockToken 컨트롤러가 잡은 일봉 적재 잠금. 끝나면 푼다 */
    @Async("dailyPriceTaskExecutor")
    public void backfillAsync(BackfillPlan plan, String lockToken) {
        try {
            BackfillResult result = backfill(plan, lockToken);
            log.info("일봉 백필 종료. from={} to={} 종목={} 저장(신규+갱신)={}행 건너뜀={}건 실패={}건 실패종목={}",
                    plan.from(), plan.to(), result.totalStocks(), result.upsertedRows(),
                    result.skippedStockCodes().size(), result.failedStockCodes().size(), result.failedStockCodes());
        } finally {
            dailyPriceJobGuard.release(lockToken);
        }
    }

    /**
     * 종목을 20개씩 나눠 Lambda를 부르고, 받은 만큼 바로 저장한다.
     * 한 묶음의 실패(Lambda 오류, 저장 오류)는 그 묶음만 실패로 기록하고 다음 묶음으로 넘어간다.
     *
     * 실패·건너뜀 판정은 DailyPriceIngestor가 한다. skipped(기간 안에 일봉이 없는 것이 정상)는 실패가 아니다.
     *
     * 묶음을 마칠 때마다 잠금 임대를 갱신한다. 잠금을 잃었으면 남은 묶음은 부르지 않고 실패로 남긴다.
     *
     * @param lockToken 일봉 적재 잠금. null이면 갱신하지 않는다(테스트 등 잠금 없이 직접 부를 때)
     */
    public BackfillResult backfill(BackfillPlan plan, String lockToken) {
        Set<String> failed = new LinkedHashSet<>();
        Set<String> skipped = new LinkedHashSet<>();
        int upsertedRows = 0;

        List<String> codes = plan.stockCodes();
        for (int start = 0; start < codes.size(); start += CHUNK_SIZE) {
            List<String> chunk = codes.subList(start, Math.min(start + CHUNK_SIZE, codes.size()));
            try {
                KrxDailyPricesByRangeResponseDto response = krxService.getDailyPricesByRange(plan.from(), plan.to(), chunk);
                Ingested ingested = dailyPriceIngestor.ingest(
                        rawRows(response), response.getSkipped(), response.getErrors(), chunk, plan.from(), plan.to());
                upsertedRows += dailyPriceIngestor.saveWithOneRetry(ingested.rows(), chunk);
                skipped.addAll(ingested.skipped());
                failed.addAll(ingested.failed());
            } catch (Exception e) {
                log.error("일봉 백필 묶음 실패. 다음 묶음으로 진행합니다. 종목={}", chunk, e);
                failed.addAll(chunk);
            }

            int next = start + CHUNK_SIZE;
            if (next < codes.size() && lockToken != null && !dailyPriceJobGuard.renew(lockToken)) {
                List<String> remaining = codes.subList(next, codes.size());
                log.error("일봉 백필을 중단합니다. 받지 못한 종목 {}건은 실패로 남깁니다.", remaining.size());
                failed.addAll(remaining);
                break;
            }
        }

        return new BackfillResult(codes.size(), upsertedRows, List.copyOf(skipped), List.copyOf(failed));
    }

    private static List<RawRow> rawRows(KrxDailyPricesByRangeResponseDto response) {
        List<RawRow> raws = new ArrayList<>();
        if (response.getPrices() == null) {
            return raws;
        }
        for (KrxDailyPricesByRangeResponseDto.StockHistory history : response.getPrices()) {
            if (history.getRows() == null) {
                continue;
            }
            for (KrxDailyPricesByRangeResponseDto.DailyRow row : history.getRows()) {
                raws.add(new RawRow(history.getStockCode(),
                        KrxService.parseTradeDateOrNull(row.getTradeDate()), row.getTradeDate(),
                        row.getOpenPrice(), row.getHighPrice(), row.getLowPrice(), row.getClosePrice()));
            }
        }
        return raws;
    }

    /** 기간은 양 끝 포함. stockCodes는 요청 순서, 중복 없음. */
    public record BackfillPlan(LocalDate from, LocalDate to, List<String> stockCodes) {

        public boolean isEmpty() {
            return stockCodes.isEmpty();
        }
    }

    /**
     * @param upsertedRows      저장한 행 수. 신규와 갱신을 합친 값이다
     * @param skippedStockCodes 기간 안에 일봉이 없는 것이 정상인 종목(장기 거래정지 등). 실패가 아니다
     * @param failedStockCodes  일봉을 받지 못했거나 일부 행을 버린 종목. 중복 없이 요청 순서.
     *                          이 종목만 stockCodes로 다시 백필하면 된다
     */
    public record BackfillResult(int totalStocks, int upsertedRows,
                                 List<String> skippedStockCodes, List<String> failedStockCodes) {
    }
}
