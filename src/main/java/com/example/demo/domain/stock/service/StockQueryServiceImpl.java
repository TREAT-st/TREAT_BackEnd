package com.example.demo.domain.stock.service;

import com.example.demo.domain.stock.entity.Stock;
import com.example.demo.domain.stock.exception.StockHandler;
import com.example.demo.domain.stock.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class StockQueryServiceImpl implements StockQueryService {

    private final StockRepository stockRepository;

    @Override
    public Stock getStockByCode(String stockCode) {
        return stockRepository.findByStockCode(stockCode)
                .orElseThrow(StockHandler::notFound);
    }

    /**
     * 편입 여부로만 좁혀 조회한다. 정렬은 pageable이 들고 온다.
     *
     * isActive가 true일 때만 편입 종목으로 좁히고, 그 외(false·null)는 전체를 조회한다.
     * isActive는 선택 파라미터라 null이 정상 입력이므로 == 비교로 언박싱하면 안 된다.
     *
     * 거래일로 좁히는 조건은 두지 않는다. Stock은 종목별 최신 스냅샷 한 건만 보관하므로
     * 과거 거래일을 조회할 대상이 애초에 없고, 최신 거래일로 좁히면 시세를 못 받아
     * 이전 거래일로 남은 종목과 아직 시세가 없는 신규 편입 종목이 목록에서 사라진다.
     * 각 종목의 기준일은 응답의 tradeDate로 전달한다.
     */
    @Override
    public Page<Stock> getAllStocks(Boolean isActive, Pageable pageable) {
        return Boolean.TRUE.equals(isActive)
                ? stockRepository.findAllByIsActive(true, pageable)
                : stockRepository.findAll(pageable);
    }

    @Override
    public Map<String, Stock> getStocksByCodes(Collection<String> stockCodes) {
        // 빈 목록으로 IN 쿼리를 날리면 DB에 따라 문법 오류가 난다. 조회할 것도 없다.
        if (stockCodes.isEmpty()) {
            return Map.of();
        }
        return stockRepository.findAllByStockCodeIn(stockCodes).stream()
                .collect(Collectors.toMap(Stock::getStockCode, Function.identity()));
    }

    @Override
    public List<String> getAllStockCodes() {
        return stockRepository.findAllStockCodes();
    }

    @Override
    public Optional<LocalDate> getLatestTradeDate() {
        return stockRepository.findLatestTradeDate();
    }
}
