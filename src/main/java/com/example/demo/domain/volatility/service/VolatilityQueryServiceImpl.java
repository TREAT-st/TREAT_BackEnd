package com.example.demo.domain.volatility.service;

import com.example.demo.domain.volatility.entity.Volatility;
import com.example.demo.domain.volatility.repository.VolatilityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class VolatilityQueryServiceImpl implements VolatilityQueryService {
    private final VolatilityRepository volatilityRepository;

    @Override
    public List<Volatility> getAllVolatilityByDate(LocalDateTime start, LocalDateTime end) {
        return volatilityRepository
                .findAllByCreatedDateGreaterThanEqualAndCreatedDateLessThanOrderByCreatedDateDesc(start, end);
    }

    @Override
    public List<Volatility> getAllVolatilityByCode(String stockCode) {
        return volatilityRepository.findAllByStockCodeOrderByCreatedDateDesc(stockCode);
    }

    @Override
    public List<Volatility> getTodayVolatility() {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = LocalDate.now().plusDays(1).atStartOfDay();
        return volatilityRepository
                .findAllByCreatedDateGreaterThanEqualAndCreatedDateLessThanOrderByCreatedDateDesc(start, end);
    }
}
