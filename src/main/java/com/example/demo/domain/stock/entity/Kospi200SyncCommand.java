package com.example.demo.domain.stock.entity;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * KRX 응답을 동기화에 필요한 형태로만 추린 것.
 * 외부 응답 DTO를 그대로 도메인까지 끌고 가지 않도록 경계에서 한 번 변환한다.
 *
 * @param incomingStocks             이번 구성종목 코드→이름
 * @param errorCodes                 Lambda가 데이터를 못 받았다고 보고한 코드.
 *                                   목록에서 빠진 종목이 이탈인지 일시적 실패인지 가를 때 쓴다.
 * @param priceSnapshots             시가·종가를 받은 종목
 * @param priceUnavailableStockCodes 목록에는 있으나 시가·종가가 비어 온 종목
 */
public record Kospi200SyncCommand(
        LocalDate tradeDate,
        Map<String, String> incomingStocks,
        Set<String> errorCodes,
        List<StockPriceSnapshot> priceSnapshots,
        List<String> priceUnavailableStockCodes
) {
}
