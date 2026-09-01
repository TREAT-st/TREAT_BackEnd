package com.example.demo.api.krx.service;

import com.example.demo.api.krx.client.KrxLambdaClient;
import com.example.demo.api.krx.dto.KrxKospi200ResponseDto;
import com.example.demo.api.krx.dto.KrxOhlcvResponseDto;
import com.example.demo.api.krx.exception.KrxHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

@Slf4j
@Service
@RequiredArgsConstructor
public class KrxService {

    /**
     * yyyyMMdd. STRICT 해석이라 20260231 같은 존재하지 않는 날짜를 보정하지 않고 거부한다.
     * STRICT에서는 연도 필드로 yyyy(연호 기준) 대신 uuuu(proleptic)를 써야 한다.
     */
    private static final DateTimeFormatter TRADE_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("uuuuMMdd").withResolverStyle(ResolverStyle.STRICT);

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
            LocalDate.parse(tradeDate, TRADE_DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            log.error("KRX Lambda 응답의 {} 형식이 올바르지 않습니다. value={}", fieldName, tradeDate);
            throw KrxHandler.krxLambdaResponseParseError();
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
