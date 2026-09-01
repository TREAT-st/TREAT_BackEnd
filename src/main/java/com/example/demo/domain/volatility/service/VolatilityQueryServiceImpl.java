package com.example.demo.domain.volatility.service;

import com.example.demo.domain.volatility.entity.Volatility;
import com.example.demo.domain.volatility.repository.VolatilityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class VolatilityQueryServiceImpl implements VolatilityQueryService {
    private final VolatilityRepository volatilityRepository;

    @Override
    public List<Volatility> getByTradeDate(LocalDate tradeDate) {
        return volatilityRepository.findAllByTradeDateOrderByIdAsc(tradeDate);
    }

    @Override
    public List<Volatility> getAllVolatilityByCode(String stockCode) {
        return volatilityRepository.findAllByStockCodeOrderByTradeDateDesc(stockCode);
    }

    /**
     * 탐지 기록이 있는 가장 최근 거래일의 종목들.
     * "오늘"을 서버 시각으로 판단하면 휴장일이거나 KRX가 전 거래일을 내려준 경우 빈 목록이 되므로,
     * 실제로 저장된 최근 거래일을 기준으로 삼는다.
     */
    @Override
    public List<Volatility> getLatestVolatility() {
        return volatilityRepository.findFirstByOrderByTradeDateDesc()
                .map(latest -> volatilityRepository.findAllByTradeDateOrderByIdAsc(latest.getTradeDate()))
                .orElseGet(List::of);
    }
}
