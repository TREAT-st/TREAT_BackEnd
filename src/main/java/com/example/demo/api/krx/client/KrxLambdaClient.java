package com.example.demo.api.krx.client;

import com.example.demo.api.krx.dto.KrxDailyPricesByDateResponseDto;
import com.example.demo.api.krx.dto.KrxKospi200ResponseDto;
import com.example.demo.api.krx.dto.KrxOhlcvResponseDto;
import com.example.demo.api.krx.dto.KrxDailyPricesByRangeResponseDto;
import com.example.demo.api.krx.exception.KrxHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class KrxLambdaClient {

    private final LambdaClient lambdaClient;
    private final ObjectMapper objectMapper;

    @Value("${krx.lambda.function-name}")
    private String ohlcvFunctionName;

    @Value("${krx.lambda.kospi200-function-name}")
    private String kospi200FunctionName;

    @Value("${krx.lambda.daily-price-function-name}")
    private String dailyPriceFunctionName;

    /** 시총 상위 종목의 최근 60거래일 OHLCV. 변동성 탐지용. */
    public KrxOhlcvResponseDto invokeGetTopMarketCapOhlcv(int topN) {
        return invoke(ohlcvFunctionName, Map.of("top", topN), KrxOhlcvResponseDto.class);
    }

    /** 코스피200 구성종목과 가장 가까운 거래일의 시가·종가. 종목 목록 동기화 + 시세 갱신용. */
    public KrxKospi200ResponseDto invokeGetKospi200Prices() {
        return invoke(kospi200FunctionName, Map.of(), KrxKospi200ResponseDto.class);
    }

    /**
     * 종목별 기간 일봉. 차트용 별도 Lambda의 기간 조회 모드다. 3년치 백필용. 코스피200 동기화 Lambda와 나눠, 차트 쪽 변경·장애·설정이
     * 매일 동기화에 영향을 주지 않게 했다.
     * 동기 호출 응답 상한(6MB) 때문에 호출자가 종목을 나눠서 보내야 한다.
     *
     * @param from yyyyMMdd
     * @param to   yyyyMMdd (오늘보다 이전)
     */
    public KrxDailyPricesByRangeResponseDto invokeGetDailyPricesByRange(String from, String to, List<String> stockCodes) {
        Map<String, Object> payload = Map.of("from", from, "to", to, "tickers", stockCodes);
        return invoke(dailyPriceFunctionName, payload, KrxDailyPricesByRangeResponseDto.class);
    }

    /**
     * 한 날짜의 종목별 일봉. 같은 Lambda의 날짜 조회 모드로, 시장 전체를 한 번에 조회한다. 일일 적재용.
     *
     * @param date yyyyMMdd (오늘보다 이전)
     */
    public KrxDailyPricesByDateResponseDto invokeGetDailyPricesByDate(String date, List<String> stockCodes) {
        Map<String, Object> payload = Map.of("date", date, "tickers", stockCodes);
        return invoke(dailyPriceFunctionName, payload, KrxDailyPricesByDateResponseDto.class);
    }

    private <T> T invoke(String functionName, Object requestPayload, Class<T> responseType) {
        String payload;
        try {
            payload = objectMapper.writeValueAsString(requestPayload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("KRX Lambda 요청 페이로드 생성 실패", e);
        }

        InvokeResponse response;
        try {
            response = lambdaClient.invoke(InvokeRequest.builder()
                    .functionName(functionName)
                    .payload(SdkBytes.fromUtf8String(payload))
                    .build());
        } catch (SdkException e) {
            log.error("KRX Lambda 호출 실패. functionName={}, payload={}", functionName, payload, e);
            throw KrxHandler.krxLambdaInvokeError();
        }

        if (response.functionError() != null) {
            log.error("KRX Lambda 실행 오류. functionName={}, functionError={}, payload={}",
                    functionName, response.functionError(), response.payload().asUtf8String());
            throw KrxHandler.krxLambdaExecutionError();
        }

        try {
            return objectMapper.readValue(response.payload().asUtf8String(), responseType);
        } catch (JsonProcessingException e) {
            log.error("KRX Lambda 응답 파싱 실패. functionName={}, payload={}",
                    functionName, response.payload().asUtf8String(), e);
            throw KrxHandler.krxLambdaResponseParseError();
        }
    }
}
