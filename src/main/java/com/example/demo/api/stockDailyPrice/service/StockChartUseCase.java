package com.example.demo.api.stockDailyPrice.service;

import com.example.demo.api.stockDailyPrice.dto.StockDailyPriceResponseDto.StockChartResponse;
import com.example.demo.api.stockDailyPrice.mapper.StockDailyPriceConverter;
import com.example.demo.common.annotation.UseCase;
import com.example.demo.domain.stock.entity.Stock;
import com.example.demo.domain.stock.service.StockQueryService;
import com.example.demo.domain.stockDailyPrice.entity.ChartRange;
import com.example.demo.domain.stockDailyPrice.entity.StockDailyPrice;
import com.example.demo.domain.stockDailyPrice.service.StockDailyPriceQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@UseCase
@RequiredArgsConstructor
public class StockChartUseCase {

    private final StockQueryService stockQueryService;
    private final StockDailyPriceQueryService stockDailyPriceQueryService;

    /**
     * 종목 차트. 모든 기간이 일봉이다. DB의 일봉만 읽고 KRX는 호출하지 않는다.
     *
     * 종목 존재 확인과 종목명은 stock에서 가져온다. 일봉만으로는 없는 종목과 아직 일봉이 없는 종목을 구분할 수 없다.
     * 기간은 [끝 − 기간, 끝]이고 양 끝을 포함한다. 끝은 오늘이 아니라 그 종목에 실제로 쌓인 마지막 거래일이다.
     * 오늘 시세는 장이 끝나야 확정되어 다음 날 적재되므로, 오늘로 세면 1W가 하루 이틀 비어 보인다.
     * 아직 일봉이 하나도 없으면(백필 전) 빈 배열로 응답한다. 종목 자체가 없으면 404(4250)다.
     */
    @Transactional(readOnly = true)
    public StockChartResponse getStockChart(String stockCode, ChartRange range) {
        Stock stock = stockQueryService.getStockByCode(stockCode);

        List<StockDailyPrice> dailyPrices = stockDailyPriceQueryService.getLatestDailyPriceDate(stockCode)
                .map(end -> stockDailyPriceQueryService.getDailyPrices(stockCode, range.startDate(end), end))
                .orElseGet(List::of);

        return StockDailyPriceConverter.toStockChartResponse(stock, range, dailyPrices);
    }
}
