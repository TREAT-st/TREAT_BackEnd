package com.example.demo.api.krx.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GetOpenAndClosePriceFromKrx Lambda의 응답 계약 검증.
 *
 * 이 Lambda는 구성종목 목록과 시세를 분리해서 내려준다.
 * 시세를 못 받은 종목도 stocks에 남고 openPrice/closePrice만 null이며,
 * 사유는 errors에 따로 담긴다. 종목명조차 못 받은 종목만 stocks에서 빠진다.
 */
class KrxKospi200ResponseDtoTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** Lambda가 실제로 내려주는 형태. 시세 정상 / 거래정지 / 시세없음 / 종목명실패를 모두 담았다. */
    private static final String LAMBDA_RESPONSE = """
            {
              "tradeDate": "20260810",
              "requestedCount": 4,
              "stocks": [
                { "stockCode": "005930", "stockName": "삼성전자",
                  "openPrice": 71000, "closePrice": 72500 },
                { "stockCode": "000660", "stockName": "SK하이닉스",
                  "openPrice": null, "closePrice": null },
                { "stockCode": "035420", "stockName": "NAVER",
                  "openPrice": null, "closePrice": null }
              ],
              "errors": [
                { "stockCode": "000660", "reason": "거래정지 또는 시세 0" },
                { "stockCode": "035420", "reason": "시세 데이터 없음" },
                { "stockCode": "123456", "reason": "종목명 조회 실패" }
              ]
            }
            """;

    @Test
    void Lambda_응답이_DTO로_매핑된다() throws Exception {
        KrxKospi200ResponseDto response = objectMapper.readValue(LAMBDA_RESPONSE, KrxKospi200ResponseDto.class);

        assertThat(response.getTradeDate()).isEqualTo("20260810");
        assertThat(response.getRequestedCount()).isEqualTo(4);
        assertThat(response.getStocks()).hasSize(3);
        assertThat(response.getErrors()).hasSize(3);
    }

    @Test
    void 시세를_못_받은_종목도_목록에_남고_가격만_null이다() throws Exception {
        KrxKospi200ResponseDto response = objectMapper.readValue(LAMBDA_RESPONSE, KrxKospi200ResponseDto.class);

        assertThat(response.getStocks())
                .extracting(KrxKospi200ResponseDto.StockPrice::getStockCode)
                .containsExactly("005930", "000660", "035420");

        assertThat(response.getStocks())
                .filteredOn(s -> !s.hasPrice())
                .extracting(KrxKospi200ResponseDto.StockPrice::getStockName)
                .containsExactly("SK하이닉스", "NAVER");
    }

    @Test
    void hasPrice가_시세_유무를_가른다() throws Exception {
        KrxKospi200ResponseDto response = objectMapper.readValue(LAMBDA_RESPONSE, KrxKospi200ResponseDto.class);

        Map<String, KrxKospi200ResponseDto.StockPrice> byCode = response.getStocks().stream()
                .collect(Collectors.toMap(KrxKospi200ResponseDto.StockPrice::getStockCode, s -> s));

        assertThat(byCode.get("005930").hasPrice()).isTrue();
        assertThat(byCode.get("005930").getOpenPrice()).isEqualByComparingTo("71000");
        assertThat(byCode.get("005930").getClosePrice()).isEqualByComparingTo("72500");

        assertThat(byCode.get("000660").hasPrice()).isFalse();
        assertThat(byCode.get("000660").getOpenPrice()).isNull();
    }

    /**
     * 종목 분류(신규/기존/편출 보류)는 DB와 비교해야 알 수 있으므로 syncStocks가 판정한다.
     * 이 DTO가 보장해야 하는 건 두 가지뿐이다 - 가격 유무가 hasPrice로 갈리는 것, errors가 온전히 파싱되는 것.
     */
    @Test
    void 시세_갱신_대상은_hasPrice로_갈린다() throws Exception {
        KrxKospi200ResponseDto response = objectMapper.readValue(LAMBDA_RESPONSE, KrxKospi200ResponseDto.class);

        assertThat(response.getStocks().stream().filter(KrxKospi200ResponseDto.StockPrice::hasPrice))
                .extracting(KrxKospi200ResponseDto.StockPrice::getStockCode)
                .containsExactly("005930");

        assertThat(response.getStocks().stream().filter(s -> !s.hasPrice()))
                .extracting(KrxKospi200ResponseDto.StockPrice::getStockCode)
                .containsExactlyInAnyOrder("000660", "035420");
    }

    @Test
    void errors는_가공_없이_그대로_파싱된다() throws Exception {
        KrxKospi200ResponseDto response = objectMapper.readValue(LAMBDA_RESPONSE, KrxKospi200ResponseDto.class);

        assertThat(response.getErrors())
                .extracting(KrxKospi200ResponseDto.ErrorItem::getStockCode)
                .containsExactlyInAnyOrder("000660", "035420", "123456");
        assertThat(response.getErrors())
                .allSatisfy(e -> assertThat(e.getReason()).isNotBlank());
    }
}
