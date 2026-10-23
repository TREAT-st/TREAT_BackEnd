package com.example.demo.api.stockDailyPrice.service;

import com.example.demo.api.krx.dto.KrxStockIssueDto;
import com.example.demo.domain.stockDailyPrice.entity.DailyPriceRow;
import com.example.demo.domain.stockDailyPrice.service.StockDailyPriceCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Lambda 응답을 검증하고 저장하는 공통 단계. 백필과 일일 적재가 같이 쓴다.
 *
 * Lambda 응답은 그대로 믿지 않는다. 일봉은 Stock과 FK가 없어 DB가 막아주지 않으므로,
 * 요청한 종목·기간 밖의 행과 가격이 무효인 행은 여기서 버린다.
 * 실패 종목은 Lambda의 errors에 기대지 않고, 검증 결과로 직접 판정한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class DailyPriceIngestor {

    /** 요약 로그에 남기는 버린 행 예시 수. 3년치 응답이면 행마다 로그를 찍을 수 없다. */
    private static final int DROPPED_EXAMPLE_LIMIT = 3;

    private final StockDailyPriceCommandService stockDailyPriceCommandService;

    /**
     * Lambda 응답 한 행.
     *
     * @param tradeDate    해석한 거래일. 형식이 틀렸으면 null
     * @param rawTradeDate 응답에 온 그대로의 거래일. 로그용
     */
    record RawRow(String stockCode, LocalDate tradeDate, String rawTradeDate,
                  BigDecimal openPrice, BigDecimal highPrice, BigDecimal lowPrice, BigDecimal closePrice) {
    }

    /**
     * @param rows      저장할 행
     * @param failed    요청한 종목 중 실패로 볼 종목. 중복 없이 요청 순서
     * @param skipped   요청한 종목 중 가격이 없는 것이 정상인 종목. 요청 순서
     */
    record Ingested(List<DailyPriceRow> rows, List<String> failed, List<String> skipped) {
    }

    /**
     * 응답을 검증해 저장할 행, 실패 종목, 건너뛴 종목으로 나눈다.
     *
     * 저장할 행: 요청한 종목이고, 거래일이 [from, to] 안이고, DailyPriceRow.isValid()를 통과한 행.
     * 실패 종목:
     *   - Lambda가 errors로 알린 종목
     *   - 요청했는데 받지도 건너뛰지도 않은 종목(errors에 없어도)
     *   - 일부 행이라도 버린 종목. 유효한 행은 저장되므로, 버린 날을 다시 받으려면 드러나야 한다
     *   - prices와 skipped, 또는 prices·skipped와 errors에 함께 있는 종목. 어느 쪽이 맞는지 알 수 없어
     *     저장하지 않는다. errors가 함께 있으면 건너뜀이 아니라 실패로 본다
     * 요청하지 않은 종목의 행은 버리기만 하고 실패로 치지 않는다(애초에 받을 종목이 아니다).
     */
    Ingested ingest(Collection<RawRow> raws, List<KrxStockIssueDto> skippedIssues, List<KrxStockIssueDto> errorIssues,
                    List<String> requestedCodes, LocalDate from, LocalDate to) {
        Set<String> requested = Set.copyOf(requestedCodes);
        Set<String> skippedSet = issueCodes(skippedIssues, requested);
        Set<String> errorSet = issueCodes(errorIssues, requested);
        Set<String> conflicting = new LinkedHashSet<>();
        raws.stream().map(RawRow::stockCode)
                .filter(code -> skippedSet.contains(code) || errorSet.contains(code))
                .forEach(conflicting::add);

        List<DailyPriceRow> rows = new ArrayList<>();
        Set<String> received = new LinkedHashSet<>();
        Set<String> partiallyDropped = new LinkedHashSet<>();
        List<String> examples = new ArrayList<>();
        int unrequested = 0, conflict = 0, badDate = 0, outOfRange = 0, invalidPrice = 0;

        for (RawRow raw : raws) {
            String code = raw.stockCode();
            if (!requested.contains(code)) {
                unrequested++;
                continue;
            }
            String reason;
            if (conflicting.contains(code)) {
                conflict++;
                reason = "가격과건너뜀·오류가함께옴";
            } else if (raw.tradeDate() == null) {
                badDate++;
                reason = "거래일형식오류";
            } else if (raw.tradeDate().isBefore(from) || raw.tradeDate().isAfter(to)) {
                outOfRange++;
                reason = "기간밖";
            } else {
                DailyPriceRow candidate = new DailyPriceRow(code, raw.tradeDate(),
                        raw.openPrice(), raw.highPrice(), raw.lowPrice(), raw.closePrice());
                if (candidate.isValid()) {
                    rows.add(candidate);
                    received.add(code);
                    continue;
                }
                invalidPrice++;
                reason = "가격무효";
            }
            partiallyDropped.add(code);
            if (examples.size() < DROPPED_EXAMPLE_LIMIT) {
                examples.add(code + ":" + raw.rawTradeDate() + ":" + reason);
            }
        }

        if (unrequested + conflict + badDate + outOfRange + invalidPrice > 0) {
            log.warn("Lambda 일봉 응답에서 저장하지 않을 행을 버렸습니다. from={} to={} 요청하지않은종목={}행 "
                            + "가격과건너뜀·오류가함께옴={}행 거래일형식오류={}행 기간밖={}행 가격무효={}행 영향종목={} 예시={}",
                    from, to, unrequested, conflict, badDate, outOfRange, invalidPrice, partiallyDropped, examples);
        }

        List<String> failed = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        for (String code : new LinkedHashSet<>(requestedCodes)) {
            boolean isSkipped = skippedSet.contains(code) && !conflicting.contains(code) && !errorSet.contains(code);
            if (errorSet.contains(code) || partiallyDropped.contains(code) || (!received.contains(code) && !isSkipped)) {
                failed.add(code);
            } else if (isSkipped) {
                skipped.add(code);
            }
        }
        return new Ingested(rows, failed, skipped);
    }

    /** 요청한 종목 중 Lambda가 skipped·errors로 알린 종목. 요청하지 않은 코드는 무시한다. */
    private static Set<String> issueCodes(List<KrxStockIssueDto> issues, Set<String> requested) {
        Set<String> codes = new LinkedHashSet<>();
        if (issues == null) {
            return codes;
        }
        issues.stream()
                .map(KrxStockIssueDto::getStockCode)
                .filter(Objects::nonNull)
                .filter(requested::contains)
                .forEach(codes::add);
        return codes;
    }

    /**
     * 다른 서버의 실행이 같은 행을 먼저 넣으면 유니크 제약 위반이 난다.
     * 다시 시도하면 그 행이 보여 갱신만 일어나므로 한 번이면 충분하다. 두 번째도 실패하면 예외가 그대로 나간다.
     *
     * @return 저장한 행 수(신규+갱신)
     */
    int saveWithOneRetry(List<DailyPriceRow> rows, Object label) {
        if (rows.isEmpty()) {
            return 0;
        }
        try {
            return stockDailyPriceCommandService.saveDailyPrices(rows);
        } catch (DataIntegrityViolationException e) {
            log.warn("일봉 저장 중 다른 실행과 겹쳐 한 번 다시 시도합니다. 대상={}", label);
            return stockDailyPriceCommandService.saveDailyPrices(rows);
        }
    }
}
