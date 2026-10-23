package com.example.demo.domain.stock.service;

import com.example.demo.domain.stock.entity.Stock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface StockQueryService {
    Stock getStockByCode(String stockCode);
    Page<Stock> getAllStocks(Boolean isActive, Pageable pageable);

    /**
     * 종목코드로 여러 종목을 한 번에 조회해 코드→종목 맵으로 돌려준다.
     *
     * 다른 도메인이 종목코드만 들고 있을 때(관심종목 등) 시세를 붙이려고 쓴다.
     * 종목마다 조회하면 N+1이 되므로 IN 쿼리 한 번으로 끝낸다.
     * 편출(비활성) 종목도 포함한다. 관심종목은 편출 후에도 남아 있을 수 있다.
     * stock에 없는 코드는 맵에서 빠진다.
     */
    Map<String, Stock> getStocksByCodes(Collection<String> stockCodes);

    /** 저장된 모든 종목코드. 편출 종목 포함, 코드 오름차순. */
    List<String> getAllStockCodes();

    /** 시세가 반영된 가장 최근 거래일. 아직 아무 시세도 없으면 비어 있다. */
    Optional<LocalDate> getLatestTradeDate();
}
