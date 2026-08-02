package com.example.demo.domain.volatility.service;

import com.example.demo.domain.volatility.entity.Volatility;
import com.example.demo.domain.volatility.repository.VolatilityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class VolatilityCommandServiceImpl implements VolatilityCommandService {

    private final VolatilityRepository volatilityRepository;

    @Override
    public void saveTopVolatilityStocks(List<VolatilitySignal> topSignals) {
        List<Volatility> volatilities = topSignals.stream()
                .map(signal -> Volatility.builder()
                        .stockCode(signal.stockCode())
                        .stockName(signal.stockName())
                        .reportUrl(null)
                        .build())
                .collect(Collectors.toList());

        volatilityRepository.saveAll(volatilities);
    }
}
