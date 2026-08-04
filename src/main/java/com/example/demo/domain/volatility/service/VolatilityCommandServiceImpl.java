package com.example.demo.domain.volatility.service;

import com.example.demo.domain.volatility.entity.Volatility;
import com.example.demo.domain.volatility.entity.VolatilitySignal;
import com.example.demo.domain.volatility.exception.VolatilityHandler;
import com.example.demo.domain.volatility.repository.VolatilityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static java.time.LocalDate.now;

@Service
@Transactional
@RequiredArgsConstructor
public class VolatilityCommandServiceImpl implements VolatilityCommandService {

    private final VolatilityRepository volatilityRepository;

    @Override
    public void saveTopVolatilityStocks(List<VolatilitySignal> topSignals) {
        LocalDateTime start = now().atStartOfDay();
        LocalDateTime end = now().plusDays(1).atStartOfDay();
        volatilityRepository.deleteAllByCreatedDateBetween(start, end);

        List<Volatility> volatilities = topSignals.stream()
                .map(signal -> Volatility.builder()
                        .stockCode(signal.stockCode())
                        .stockName(signal.stockName())
                        .reportUrl(null)
                        .build())
                .collect(Collectors.toList());

        volatilityRepository.saveAll(volatilities);
    }

    @Override
    public void updateReportUrl(String stockCode, LocalDate reportDate, String reportUrl) {
        LocalDateTime start = reportDate.atStartOfDay();
        LocalDateTime end = reportDate.plusDays(1).atStartOfDay();

        Volatility volatility = volatilityRepository
                .findFirstByStockCodeAndCreatedDateGreaterThanEqualAndCreatedDateLessThanOrderByCreatedDateDesc(
                        stockCode, start, end)
                .orElseThrow(VolatilityHandler::volatilityNotFound);

        volatility.updateReportUrl(reportUrl);
    }
}
