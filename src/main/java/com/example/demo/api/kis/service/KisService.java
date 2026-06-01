package com.example.demo.api.kis.service;

import com.example.demo.api.kis.dto.KisResponseDto;
import com.example.demo.domain.stock.exception.StockHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class KisService {

    private final WebClient webClient;
    private final KisTokenService kisTokenService;

    @Value("${kis.app-key}")
    private String appKey;

    @Value("${kis.app-secret}")
    private String appSecret;

    /**
     * 일별 시가/종가 조회
     * @param stockCode 종목코드 (예: 005930)
     * @param startDate 조회 시작일 (YYYYMMDD)
     * @param endDate   조회 종료일 (YYYYMMDD)
     */
    public KisResponseDto getDailyStockPrice(String stockCode, String startDate, String endDate) {
        String token = kisTokenService.getAccessToken();

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/uapi/domestic-stock/v1/quotations/inquire-daily-itemchartprice")
                        .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                        .queryParam("FID_INPUT_ISCD", stockCode)
                        .queryParam("FID_INPUT_DATE_1", startDate)
                        .queryParam("FID_INPUT_DATE_2", endDate)
                        .queryParam("FID_PERIOD_DIV_CODE", "D")
                        .queryParam("FID_ORG_ADJ_PRC", "0")
                        .build())
                .header("authorization", "Bearer " + token)
                .header("appkey", appKey)
                .header("appsecret", appSecret)
                .header("tr_id", "FHKST03010100")  // 실전: FHKST03010100 / 모의: VFHKST03010100
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError,
                        res -> Mono.error(StockHandler.kisApiError()))
                .onStatus(HttpStatusCode::is5xxServerError,
                        res -> Mono.error(StockHandler.kisApiError()))
                .bodyToMono(KisResponseDto.class)
                .map(response -> {
                    if (!"0".equals(response.getRtCd())) {
                        throw StockHandler.kisApiError();
                    }
                    return response;
                })
                .timeout(Duration.ofSeconds(5))
                .block();
    }
}
