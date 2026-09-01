package com.example.demo.api.krx.service;

import com.example.demo.api.krx.client.KrxLambdaClient;
import com.example.demo.api.krx.dto.KrxKospi200ResponseDto;
import com.example.demo.api.krx.dto.KrxOhlcvResponseDto;
import com.example.demo.api.krx.exception.KrxHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
     * 코스피200 구성종목과 해당 거래일의 시가·종가.
     * 목록이 비어 있으면 그대로 동기화할 경우 전 종목이 비활성 처리되므로 반드시 예외로 막는다.
     */
    public KrxKospi200ResponseDto getKospi200Prices() {
        KrxKospi200ResponseDto response = krxLambdaClient.invokeGetKospi200Prices();

        if (response == null || response.getStocks() == null || response.getStocks().isEmpty()) {
            log.error("KRX 코스피200 Lambda 응답이 비어있습니다.");
            throw KrxHandler.krxLambdaResponseEmpty();
        }

        // tradeDate는 동기화의 날짜 키다. 없으면 파싱 단계에서 NPE로 터지므로 여기서 계약 위반으로 막는다.
        if (response.getTradeDate() == null || response.getTradeDate().isBlank()) {
            log.error("KRX 코스피200 Lambda 응답에 tradeDate가 없습니다.");
            throw KrxHandler.krxLambdaResponseParseError();
        }

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
