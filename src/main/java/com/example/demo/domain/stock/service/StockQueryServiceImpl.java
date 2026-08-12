package com.example.demo.domain.stock.service;

import com.example.demo.domain.stock.entity.Stock;
import com.example.demo.domain.stock.exception.StockHandler;
import com.example.demo.domain.stock.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class StockQueryServiceImpl implements StockQueryService {

    private final StockRepository stockRepository;

    /** 편출된(비활성) 종목도 조회된다. 관심종목·과거 변동성 기록이 참조할 수 있기 때문이다. */
    @Override
    public Stock getStockByCode(String stockCode) {
        return stockRepository.findByStockCode(stockCode)
                .orElseThrow(StockHandler::notFound);
    }

    /** 목록 노출은 현재 코스피200에 편입된 종목만 대상으로 한다. */
    @Override
    public Page<Stock> getAllActiveStocks(Pageable pageable) {
        return stockRepository.findAllByIsActiveTrue(pageable);
    }

    @Override
    public List<Stock> getActiveStocks() {
        return stockRepository.findAllByIsActiveTrue();
    }
}
