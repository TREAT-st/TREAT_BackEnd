package com.example.demo.api.krx.client;

import com.example.demo.api.krx.dto.KrxKospi200ResponseDto;
import com.example.demo.api.krx.dto.KrxOhlcvResponseDto;
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

import java.util.Map;

/**
 * pykrx 기반 AWS Lambda(별도 배포)를 호출하는 클라이언트.
 * Lambda 함수 자체는 이 프로젝트가 아니라 별도로 배포된다 — 이 클라이언트는 각 함수의
 * 요청/응답 계약에 맞춰 호출만 담당한다.
 */
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

    /** 시총 상위 종목의 최근 60거래일 OHLCV. 변동성 탐지용. */
    public KrxOhlcvResponseDto invokeGetTopMarketCapOhlcv(int topN) {
        return invoke(ohlcvFunctionName, Map.of("top", topN), KrxOhlcvResponseDto.class);
    }

    /** 코스피200 구성종목과 가장 가까운 거래일의 시가·종가. 종목 목록 동기화 + 시세 갱신용. */
    public KrxKospi200ResponseDto invokeGetKospi200Prices() {
        return invoke(kospi200FunctionName, Map.of(), KrxKospi200ResponseDto.class);
    }

    private <T> T invoke(String functionName, Object requestPayload, Class<T> responseType) {
        String payload;
        try {
            payload = objectMapper.writeValueAsString(requestPayload);
        } catch (JsonProcessingException e) {
            // 고정 구조라 실패할 수 없다. 발생했다면 우리 코드 버그이므로 500으로 내보낸다.
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
