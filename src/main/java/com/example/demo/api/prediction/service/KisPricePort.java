package com.example.demo.api.prediction.service;

import com.example.demo.api.kis.dto.KisResponseDto;
import com.example.demo.api.kis.service.KisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * KIS 가격 조회 어댑터.
 * KIS 연동이 아직 403이라, getCurrentPrice는 고정값을 반환 중.
 * getClosePriceAt은 KIS 일봉 API를 실제 호출하되, 실패 시 고정값으로 fallback.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KisPricePort {

    private static final DateTimeFormatter KIS_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final BigDecimal FALLBACK_PRICE = new BigDecimal("218000");

    private final KisService kisService;

    /** 현재가 조회 (예측 제출 시점용). KIS 연동 전까지 고정값 반환. */
    public BigDecimal getCurrentPrice(String stockCode) {
        // TODO: KIS 현재가 API 연동 후 실제 호출로 교체
        return FALLBACK_PRICE;
    }

    /**
     * 특정 날짜의 종가 조회 (스케줄러 채점용).
     * KIS 일봉 API 호출 → output2[0].closePrice 사용.
     * 실패 시 fallback 가격 반환 후 로그 출력.
     */
    public BigDecimal getClosePriceAt(String stockCode, LocalDate date) {
        String dateStr = date.format(KIS_DATE_FORMAT);
        try {
            KisResponseDto response = kisService.getDailyStockPrice(stockCode, dateStr, dateStr);
            List<KisResponseDto.DailyData> output = response.getOutput2();
            if (output != null && !output.isEmpty()) {
                String closePrice = output.get(0).getClosePrice();
                return new BigDecimal(closePrice);
            }
            log.warn("KIS 일봉 데이터 없음. stockCode={}, date={}", stockCode, dateStr);
        } catch (Exception e) {
            log.warn("KIS 종가 조회 실패 - fallback 사용. stockCode={}, date={}, error={}",
                    stockCode, dateStr, e.getMessage());
        }
        return FALLBACK_PRICE;
    }
}
