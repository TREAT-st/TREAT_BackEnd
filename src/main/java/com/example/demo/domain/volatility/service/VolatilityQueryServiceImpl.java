package com.example.demo.domain.volatility.service;

import com.example.demo.domain.volatility.entity.Volatility;
import com.example.demo.domain.volatility.exception.VolatilityHandler;
import com.example.demo.domain.volatility.repository.VolatilityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class VolatilityQueryServiceImpl implements VolatilityQueryService {
    private final VolatilityRepository volatilityRepository;

    @Override
    public List<Volatility> getAllVolatility() {
        return volatilityRepository.findAllByOrderByCreatedDateDesc();
    }

    @Override
    public Volatility getVolatilityByCode(String stockCode) {
        return volatilityRepository.findByStockCode(stockCode)
                .orElseThrow(() -> VolatilityHandler.NOT_FOUND);
    }
}
