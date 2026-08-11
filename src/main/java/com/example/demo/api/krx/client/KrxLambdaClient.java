package com.example.demo.api.krx.client;

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
 * KOSPI 시총 상위 종목의 OHLCV를 pykrx로 조회하는 AWS Lambda(별도 배포)를 호출하는 클라이언트.
 * Lambda 함수 자체는 이 프로젝트가 아니라 별도로 배포된다 — 이 클라이언트는 그 함수의
 * 요청/응답 계약({@link KrxOhlcvResponseDto})에 맞춰 호출만 담당한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KrxLambdaClient {

    private final LambdaClient lambdaClient;
    private final ObjectMapper objectMapper;

    @Value("${krx.lambda.function-name}")
    private String functionName;

    public KrxOhlcvResponseDto invokeGetTopMarketCapOhlcv(int topN) {
        String payload;
        try {
            payload = objectMapper.writeValueAsString(Map.of("top", topN));
        } catch (JsonProcessingException e) {
            // 고정 구조라 실패할 수 없다. 발생했다면 우리 코드 버그이므로 500으로 내보낸다.
            throw new IllegalStateException("KRX Lambda 요청 페이로드 생성 실패", e);
        }

        InvokeRequest request = InvokeRequest.builder()
                .functionName(functionName)
                .payload(SdkBytes.fromUtf8String(payload))
                .build();

        InvokeResponse response;
        try {
            response = lambdaClient.invoke(request);
        } catch (SdkException e) {
            log.error("KRX Lambda 호출 실패. functionName={}, topN={}", functionName, topN, e);
            throw KrxHandler.krxLambdaInvokeError();
        }

        if (response.functionError() != null) {
            log.error("KRX Lambda 실행 오류. functionError={}, payload={}",
                    response.functionError(), response.payload().asUtf8String());
            throw KrxHandler.krxLambdaExecutionError();
        }

        try {
            return objectMapper.readValue(response.payload().asUtf8String(), KrxOhlcvResponseDto.class);
        } catch (JsonProcessingException e) {
            log.error("KRX Lambda 응답 파싱 실패. payload={}", response.payload().asUtf8String(), e);
            throw KrxHandler.krxLambdaResponseParseError();
        }
    }
}
