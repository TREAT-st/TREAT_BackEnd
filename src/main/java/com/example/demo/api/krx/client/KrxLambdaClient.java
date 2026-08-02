package com.example.demo.api.krx.client;

import com.example.demo.api.krx.dto.KrxOhlcvResponseDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;

import java.util.Map;

/**
 * KOSPI 시총 상위 종목의 OHLCV를 pykrx로 조회하는 AWS Lambda(별도 배포)를 호출하는 클라이언트.
 * Lambda 함수 자체는 이 프로젝트가 아니라 별도로 배포된다 — 이 클라이언트는 그 함수의
 * 요청/응답 계약({@link KrxOhlcvResponseDto})에 맞춰 호출만 담당한다.
 */
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
            throw new IllegalStateException("KRX Lambda 요청 페이로드 생성 실패", e);
        }

        InvokeRequest request = InvokeRequest.builder()
                .functionName(functionName)
                .payload(SdkBytes.fromUtf8String(payload))
                .build();

        InvokeResponse response = lambdaClient.invoke(request);

        if (response.functionError() != null) {
            throw new IllegalStateException(
                    "KRX Lambda 실행 오류: " + response.functionError()
                            + ", payload=" + response.payload().asUtf8String());
        }

        try {
            return objectMapper.readValue(response.payload().asUtf8String(), KrxOhlcvResponseDto.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("KRX Lambda 응답 파싱 실패", e);
        }
    }
}
