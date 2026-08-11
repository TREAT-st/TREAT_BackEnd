package com.example.demo.api.kis.service;

import com.example.demo.api.kis.client.KisFeignClient;
import com.example.demo.api.kis.dto.KisResponseDto;
import com.example.demo.api.kis.exception.KisHandler;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KisService {

    private final KisFeignClient kisFeignClient;
    private final KisTokenService kisTokenService;

    @Value("${kis.app-key}")
    private String appKey;

    @Value("${kis.app-secret}")
    private String appSecret;

    public KisResponseDto getDailyStockPrice(String stockCode, String startDate, String endDate) {
        String token = kisTokenService.getAccessToken();

        try {
            KisResponseDto response = kisFeignClient.getDailyStockPrice(
                    "Bearer " + token,
                    appKey,
                    appSecret,
                    "FHKST03010100",
                    "J",
                    stockCode,
                    startDate,
                    endDate,
                    "D",
                    "0"
            );

            if (response == null) {
                log.error("KIS API 응답이 비어있습니다. stockCode={}", stockCode);
                throw KisHandler.kisResponseEmpty();
            }
            if (!"0".equals(response.getRtCd())) {
                log.error("KIS API 응답 오류. rt_cd={}, msg={}", response.getRtCd(), response.getMsg1());
                throw KisHandler.kisApiError();
            }

            return response;

        } catch (FeignException e) {
            log.error("KIS API 호출 실패. status={}, stockCode={}", e.status(), stockCode);
            throw KisHandler.kisApiError();
        }
    }
}