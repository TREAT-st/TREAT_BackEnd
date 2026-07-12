package com.example.demo.api.prediction.service;

import com.example.demo.api.kis.service.KisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * KIS 현재가 조회 어댑터.
 * ★ KIS 연동이 아직 403이라, 지금은 고정값(218000)을 반환해 전체 플로우가 돌아가게 해둠.
 *   연동 풀리면 아래 주석의 실제 호출로 교체 예정 (KisService 실제 메서드명 확인 필요)
 */
@Component
@RequiredArgsConstructor
public class KisPricePort {

    private final KisService kisService;

    public BigDecimal getCurrentPrice(String stockCode) {
        // TODO: 실제 KisService 메서드로 교체
        // return kisService.getCurrentPrice(stockCode);
        return new BigDecimal("218000"); // 임시 고정값 (Image 1의 현재가)
    }
}
