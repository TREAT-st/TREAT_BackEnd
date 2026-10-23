package com.example.demo.api.krx.service;

import com.example.demo.api.krx.client.KrxLambdaClient;
import com.example.demo.api.krx.dto.KrxDailyPricesByDateResponseDto;
import com.example.demo.api.krx.dto.KrxKospi200ResponseDto;
import com.example.demo.api.krx.dto.KrxOhlcvResponseDto;
import com.example.demo.api.krx.dto.KrxDailyPricesByRangeResponseDto;
import com.example.demo.api.krx.exception.KrxHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

import static com.example.demo.common.consts.StaticVariable.YYYYMMDD_FORMATTER;

@Slf4j
@Service
@RequiredArgsConstructor
public class KrxService {

    private final KrxLambdaClient krxLambdaClient;

    /**
     * 호출 실패·실행 오류·응답 파싱 실패는 {@link KrxLambdaClient}가 각각의 도메인 예외로 변환해 던진다.
     * 여기서는 "응답은 왔지만 종목이 비어 있는" 경우만 판정한다.
     */
    public KrxOhlcvResponseDto getTopMarketCapOhlcv(int topN) {
        KrxOhlcvResponseDto response = krxLambdaClient.invokeGetTopMarketCapOhlcv(topN);

        if (response == null || response.getStocks() == null || response.getStocks().isEmpty()) {
            log.error("KRX Lambda 응답이 비어있습니다. topN={}", topN);
            throw KrxHandler.krxLambdaResponseEmpty();
        }

        // 거래일은 탐지 결과의 날짜 키다. 값이 없거나 형식이 틀리면 뒤에서 서버 날짜로
        // 대체되거나 파싱 예외가 500으로 새어 나가므로, 경계에서 계약 위반으로 막는다.
        requireValidTradeDate(response.getEffectiveTradeDate(), "effective_trade_date");

        // Lambda가 종목별로 실패를 격리하고 errors에 사유를 담아 보낸다. 조용히 누락되지 않도록 남긴다.
        if (response.getErrors() != null && !response.getErrors().isEmpty()) {
            log.warn("KRX Lambda가 {}건을 제외했습니다. 수신 {}건 / 요청 {}건. 사유={}",
                    response.getErrors().size(), response.getStocks().size(), topN,
                    response.getErrors().stream()
                            .map(e -> e.getStockCode() + ":" + e.getReason())
                            .toList());
        }

        return response;
    }

    /**
     * 거래일 문자열이 yyyyMMdd로 실재하는 날짜인지 확인한다.
     * 파싱은 뒤 계층에서 다시 하지만, 그쪽 예외는 도메인 예외로 변환되지 않아 500이 된다.
     */
    private void requireValidTradeDate(String tradeDate, String fieldName) {
        if (tradeDate == null || tradeDate.isBlank()) {
            log.error("KRX Lambda 응답에 {}가 없습니다.", fieldName);
            throw KrxHandler.krxLambdaResponseParseError();
        }
        try {
            LocalDate.parse(tradeDate, YYYYMMDD_FORMATTER);
        } catch (DateTimeParseException e) {
            log.error("KRX Lambda 응답의 {} 형식이 올바르지 않습니다. value={}", fieldName, tradeDate);
            throw KrxHandler.krxLambdaResponseParseError();
        }
    }

    /**
     * 종목별 기간 일봉. 3년치 백필용.
     *
     * 받은 종목도 건너뛴 종목도 없으면(prices·skipped가 모두 비었으면) 실패로 막는다. 그대로 통과시키면
     * 저장할 게 없어 0건 저장으로 조용히 성공 처리된다. 종목 일부 실패는 errors로 오고
     * 나머지는 쓸 수 있으므로 통과시킨다. 요청한 종목이 응답에서 빠졌는지는 호출자가 직접 대조한다.
     * 응답의 기간이 요청과 다르면 막는다.
     */
    public KrxDailyPricesByRangeResponseDto getDailyPricesByRange(LocalDate from, LocalDate to, List<String> stockCodes) {
        KrxDailyPricesByRangeResponseDto response = krxLambdaClient.invokeGetDailyPricesByRange(
                from.format(YYYYMMDD_FORMATTER), to.format(YYYYMMDD_FORMATTER), stockCodes);

        if (response == null || (isEmpty(response.getPrices()) && isEmpty(response.getSkipped()))) {
            log.error("KRX 일봉 응답이 비어있습니다. from={} to={} 종목={}건", from, to, stockCodes.size());
            throw KrxHandler.krxLambdaResponseEmpty();
        }
        String requestedFrom = from.format(YYYYMMDD_FORMATTER);
        String requestedTo = to.format(YYYYMMDD_FORMATTER);
        if (!requestedFrom.equals(response.getFrom()) || !requestedTo.equals(response.getTo())) {
            log.error("KRX 일봉 응답의 기간이 요청과 다릅니다. 요청={}~{} 응답={}~{}",
                    requestedFrom, requestedTo, response.getFrom(), response.getTo());
            throw KrxHandler.krxLambdaResponseMismatch();
        }

        if (!isEmpty(response.getErrors())) {
            log.warn("KRX 일봉 {}건 실패. 사유={}", response.getErrors().size(),
                    response.getErrors().stream()
                            .map(e -> e.getStockCode() + ":" + e.getReason())
                            .toList());
        }
        return response;
    }

    /**
     * 한 날짜의 종목별 일봉. 일일 적재용.
     *
     * 휴장일(marketClosed)이나 요청 종목이 모두 거래정지인 경우처럼 prices가 비는 것이 정상일 수 있어
     * 비어 있다고 막지 않는다. 요청한 종목이 빠졌는지는 호출자가 대조해 실패로 기록한다.
     * 응답 자체가 없거나, 응답 날짜가 요청과 다르거나, 휴장일이라면서 시세가 있으면 막는다.
     */
    public KrxDailyPricesByDateResponseDto getDailyPricesByDate(LocalDate date, List<String> stockCodes) {
        KrxDailyPricesByDateResponseDto response = krxLambdaClient.invokeGetDailyPricesByDate(
                date.format(YYYYMMDD_FORMATTER), stockCodes);

        if (response == null) {
            log.error("KRX 날짜별 일봉 응답이 비어있습니다. date={} 종목={}건", date, stockCodes.size());
            throw KrxHandler.krxLambdaResponseEmpty();
        }
        String requestedDate = date.format(YYYYMMDD_FORMATTER);
        if (!requestedDate.equals(response.getDate())) {
            // 그대로 받으면 다른 날짜의 시세가 요청 날짜로 저장된다(날짜 모드 응답의 행에는 거래일이 없다).
            log.error("KRX 날짜별 일봉 응답의 날짜가 요청과 다릅니다. 요청={} 응답={}", requestedDate, response.getDate());
            throw KrxHandler.krxLambdaResponseMismatch();
        }
        if (response.isMarketClosed() && !isEmpty(response.getPrices())) {
            log.error("KRX 날짜별 일봉 응답이 휴장일이라면서 시세 {}건을 담고 있습니다. date={}",
                    response.getPrices().size(), requestedDate);
            throw KrxHandler.krxLambdaResponseMismatch();
        }
        return response;
    }

    private static boolean isEmpty(List<?> list) {
        return list == null || list.isEmpty();
    }

    /** 일봉 행의 거래일. 형식이 틀리면 null(호출자가 그 행을 버린다). */
    public static LocalDate parseTradeDateOrNull(String tradeDate) {
        try {
            return tradeDate == null ? null : LocalDate.parse(tradeDate, YYYYMMDD_FORMATTER);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /**
     * 코스피200 구성종목과 해당 거래일의 시가·종가.
     * 목록이 비어 있으면 그대로 동기화할 경우 전 종목이 비활성 처리되므로 반드시 예외로 막는다.
     */
    public KrxKospi200ResponseDto getKospi200Prices() {
        KrxKospi200ResponseDto response = krxLambdaClient.invokeGetKospi200Prices();

        if (response == null || response.getStocks() == null || response.getStocks().isEmpty()) {
            log.error("KRX 코스피200 Lambda 응답이 비어있습니다.");
            throw KrxHandler.krxLambdaResponseEmpty();
        }

        // tradeDate는 동기화의 날짜 키다. 비어 있으면 NPE, 형식이 틀리면 DateTimeParseException이
        // 그대로 500으로 새어 나가므로 여기서 계약 위반으로 막는다.
        requireValidTradeDate(response.getTradeDate(), "tradeDate");

        if (response.getErrors() != null && !response.getErrors().isEmpty()) {
            log.warn("KRX 코스피200 Lambda가 {}건을 제외했습니다. 수신 {}건 / 구성종목 {}건. 사유={}",
                    response.getErrors().size(), response.getStocks().size(), response.getRequestedCount(),
                    response.getErrors().stream()
                            .map(e -> e.getStockCode() + ":" + e.getReason())
                            .toList());
        }

        return response;
    }
}
