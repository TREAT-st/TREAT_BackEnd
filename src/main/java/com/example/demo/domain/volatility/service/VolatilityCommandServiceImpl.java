package com.example.demo.domain.volatility.service;

import com.example.demo.domain.volatility.entity.Volatility;
import com.example.demo.domain.volatility.entity.VolatilitySignal;
import com.example.demo.domain.volatility.exception.VolatilityHandler;
import com.example.demo.domain.volatility.repository.VolatilityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class VolatilityCommandServiceImpl implements VolatilityCommandService {

    private final VolatilityRepository volatilityRepository;

    /**
     * 같은 거래일에 다시 실행해도 결과가 같도록 해당 거래일의 집합을 교체한다.
     * 단 전량 삭제 후 재삽입하면 콜백으로 채워진 reportUrl이 유실되므로,
     * 이미 있는 종목은 갱신하고 새 종목만 추가한다.
     */
    @Override
    public void saveTopVolatilityStocks(List<VolatilitySignal> topSignals, LocalDate tradeDate) {
        // 탐지 결과가 비었는데 기존 기록을 지우면 그날 이력이 통째로 사라진다.
        if (topSignals.isEmpty()) {
            log.warn("저장할 변동성 종목이 없습니다. 기존 기록을 유지합니다. tradeDate={}", tradeDate);
            return;
        }

        Map<String, Volatility> existingByCode = volatilityRepository.findAllByTradeDateOrderByIdAsc(tradeDate)
                .stream()
                .collect(Collectors.toMap(Volatility::getStockCode, Function.identity()));

        Set<String> selectedCodes = new LinkedHashSet<>();
        List<Volatility> toInsert = new ArrayList<>();

        for (VolatilitySignal signal : topSignals) {
            selectedCodes.add(signal.stockCode());

            Volatility existing = existingByCode.get(signal.stockCode());
            if (existing != null) {
                // reportUrl은 건드리지 않는다. 변경 감지로 종목명만 갱신된다.
                existing.updateStockName(signal.stockName());
                continue;
            }

            toInsert.add(Volatility.builder()
                    .stockCode(signal.stockCode())
                    .stockName(signal.stockName())
                    .tradeDate(tradeDate)
                    .reportUrl(null)
                    .build());
        }

        // 재실행으로 상위 목록에서 빠진 종목. 그날의 상위 종목이 아니므로 남겨둘 이유가 없다.
        List<Volatility> dropped = existingByCode.values().stream()
                .filter(v -> !selectedCodes.contains(v.getStockCode()))
                .toList();

        if (!dropped.isEmpty()) {
            volatilityRepository.deleteAll(dropped);
        }
        volatilityRepository.saveAll(toInsert);

        log.info("변동성 상위 종목 저장 완료. tradeDate={} 선정={} 신규={} 갱신={} 제외={}",
                tradeDate, topSignals.size(), toInsert.size(),
                topSignals.size() - toInsert.size(), dropped.size());
    }

    @Override
    public void updateReportUrl(String stockCode, LocalDate tradeDate, String reportUrl) {
        Volatility volatility = volatilityRepository.findByStockCodeAndTradeDate(stockCode, tradeDate)
                .orElseThrow(VolatilityHandler::volatilityNotFound);

        volatility.updateReportUrl(reportUrl);
    }
}
