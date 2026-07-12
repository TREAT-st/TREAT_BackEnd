package com.example.demo.api.prediction.service;

import com.example.demo.domain.stock.entity.Stock;
import com.example.demo.domain.stock.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Stock 조회 어댑터.
 * ★ findByStockCode 는 StockRepository의 실제 메서드명에 맞추세요.
 *   (컨트롤러가 stockCode 를 쓰므로 컬럼/필드명 stockCode 로 가정)
 * 이미 stock 도메인에 QueryService가 있으면 이 파일 지우고 그걸 UseCase에 직접 주입해도 됩니다.
 */
@Component
@RequiredArgsConstructor
public class StockQueryPort {

    private final StockRepository stockRepository;

    public Stock getByStockCode(String stockCode) {
        return stockRepository.findByStockCode(stockCode)
                .orElseThrow(() -> new IllegalArgumentException("stock을 찾지 못 했습니다: " + stockCode));
    }
}
