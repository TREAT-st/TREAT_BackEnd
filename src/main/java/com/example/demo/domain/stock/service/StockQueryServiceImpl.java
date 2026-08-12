package com.example.demo.domain.stock.service;

import com.example.demo.domain.stock.entity.Stock;
import com.example.demo.domain.stock.exception.StockHandler;
import com.example.demo.domain.stock.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
     * isActive가 true일 때만 편입 종목으로 좁히고, 그 외(false·null)는 전체를 조회한다.
     * isActive는 선택 파라미터라 null이 정상 입력이므로 == 비교로 언박싱하면 안 된다.
     */
    @Override
    public Page<Stock> getAllStocks(Boolean isActive, Pageable pageable) {
        return Boolean.TRUE.equals(isActive)
                ? stockRepository.findAllByIsActive(true, pageable)
                : stockRepository.findAll(pageable);
    }
}
