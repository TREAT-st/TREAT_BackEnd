package com.example.demo.api.report.client;

import com.example.demo.api.report.dto.ReportLambdaRequestDto;
import com.example.demo.domain.volatility.exception.VolatilityHandler;
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

@Slf4j
@Component
@RequiredArgsConstructor
public class ReportLambdaClient {

    private final LambdaClient lambdaClient;
    private final ObjectMapper objectMapper;

    @Value("${report.lambda.function-name}")
    private String functionName;

    public void invokeCreateReport(ReportLambdaRequestDto request) {
        String payload;
        try {
            payload = objectMapper.writeValueAsString(request);
            log.info("[ReportLambda] payload: {}", payload);
        } catch (JsonProcessingException e) {
            // DTO 직렬화 실패는 우리 코드 버그이므로 500으로 내보낸다.
            throw new IllegalStateException("리포트 Lambda 요청 페이로드 생성 실패", e);
        }

        InvokeResponse response;
        try {
            response = lambdaClient.invoke(InvokeRequest.builder()
                    .functionName(functionName)
                    .payload(SdkBytes.fromUtf8String(payload))
                    .build());
        } catch (SdkException e) {
            log.error("리포트 Lambda 호출 실패. functionName={}, stockCode={}",
                    functionName, request.getStockCode(), e);
            throw VolatilityHandler.reportLambdaInvokeError();
        }

        if (response.functionError() != null) {
            log.error("리포트 Lambda 실행 오류. stockCode={}, functionError={}, payload={}",
                    request.getStockCode(), response.functionError(), response.payload().asUtf8String());
            throw VolatilityHandler.reportLambdaExecutionError();
        }
    }
}
