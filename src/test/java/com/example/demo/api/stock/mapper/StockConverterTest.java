package com.example.demo.api.stock.mapper;

import com.example.demo.api.krx.dto.KrxKospi200ResponseDto;
import com.example.demo.domain.stock.entity.Kospi200SyncCommand;
import com.example.demo.domain.stock.entity.StockPriceSnapshot;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * KRX 응답을 동기화 입력으로 옮기는 경계 변환 검증.
 * 시세 유무를 errors가 아니라 hasPrice로 가르는 것이 이 변환의 핵심이다.
 */
class StockConverterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

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

    private Kospi200SyncCommand convert() throws Exception {
        return StockConverter.toKospi200SyncCommand(
                objectMapper.readValue(LAMBDA_RESPONSE, KrxKospi200ResponseDto.class));
    }

    @Test
    void 거래일은_응답의_tradeDate를_따른다() throws Exception {
        assertThat(convert().tradeDate()).isEqualTo(LocalDate.of(2026, 8, 10));
    }

    @Test
    void 구성종목은_errors와_무관하게_stocks_전체다() throws Exception {
        // errors에 있어도 stocks에 있으면 코스피200 구성종목이다. 이탈로 오판하면 안 된다.
        assertThat(convert().incomingStocks())
                .containsOnlyKeys("005930", "000660", "035420")
                .containsEntry("000660", "SK하이닉스");
    }

    @Test
    void errors는_가공_없이_코드_집합으로만_넘어간다() throws Exception {
        assertThat(convert().errorCodes())
                .containsExactlyInAnyOrder("000660", "035420", "123456");
    }

    @Test
    void 시세는_hasPrice_기준으로_갈린다() throws Exception {
        Kospi200SyncCommand command = convert();

        assertThat(command.priceSnapshots())
                .extracting(StockPriceSnapshot::stockCode)
                .containsExactly("005930");
        assertThat(command.priceUnavailableStockCodes())
                .containsExactly("000660", "035420");

        // 두 목록을 합치면 구성종목 전체가 된다. 어느 쪽에도 안 잡혀 사라지는 종목이 없어야 한다.
        assertThat(command.priceSnapshots().size() + command.priceUnavailableStockCodes().size())
                .isEqualTo(command.incomingStocks().size());
    }

    @Test
    void errors가_없어도_변환된다() throws Exception {
        KrxKospi200ResponseDto response = objectMapper.readValue("""
                { "tradeDate": "20260810", "requestedCount": 1,
                  "stocks": [ { "stockCode": "005930", "stockName": "삼성전자",
                                "openPrice": 71000, "closePrice": 72500 } ] }
                """, KrxKospi200ResponseDto.class);

        assertThat(StockConverter.toKospi200SyncCommand(response).errorCodes()).isEmpty();
    }
}
