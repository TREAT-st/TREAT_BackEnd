package com.example.demo.api.krx.service;

import com.example.demo.api.krx.client.KrxLambdaClient;
import com.example.demo.api.krx.dto.KrxDailyPricesByDateResponseDto;
import com.example.demo.api.krx.dto.KrxDailyPricesByRangeResponseDto;
import com.example.demo.api.krx.exception.KrxErrorStatus;
import com.example.demo.common.exception.GeneralException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;

/**
 * 일봉 응답의 경계 검증.
 * 받은 종목이 하나도 없는 응답을 통과시키면 0건 저장으로 조용히 성공 처리된다.
 */
class KrxServiceDailyPricesTest {

    private static final LocalDate FROM = LocalDate.of(2023, 9, 1);
    private static final LocalDate TO = LocalDate.of(2026, 10, 16);

    private final KrxLambdaClient client = Mockito.mock(KrxLambdaClient.class);
    private final KrxService krxService = new KrxService(client);
    private final ObjectMapper objectMapper = new ObjectMapper();

    private void respond(String json) throws Exception {
        Mockito.when(client.invokeGetDailyPricesByRange(anyString(), anyString(), anyList()))
                .thenReturn(json == null ? null : objectMapper.readValue(json, KrxDailyPricesByRangeResponseDto.class));
    }

    private void assertEmptyResponse() {
        assertThatThrownBy(() -> krxService.getDailyPricesByRange(FROM, TO, List.of("005930")))
                .isInstanceOf(GeneralException.class)
                .extracting(e -> ((GeneralException) e).getErrorReasonHttpStatus().getCode())
                .isEqualTo(KrxErrorStatus.KRX_LAMBDA_RESPONSE_EMPTY.getCode());
    }

    @Test
    void prices가_빈_배열이면_실패다() throws Exception {
        respond("""
                { "from": "20230901", "to": "20261016", "prices": [], "errors": [] }
                """);
        assertEmptyResponse();
    }

    @Test
    void prices가_없으면_실패다() throws Exception {
        respond("""
                { "from": "20230901", "to": "20261016", "errors": [] }
                """);
        assertEmptyResponse();
    }

    @Test
    void 응답이_null이면_실패다() throws Exception {
        respond(null);
        assertEmptyResponse();
    }

    /** 일부 종목만 실패한 응답은 나머지를 쓸 수 있으므로 통과시킨다. */
    @Test
    void 받은_종목이_있으면_일부_실패여도_통과한다() throws Exception {
        respond("""
                { "from": "20230901", "to": "20261016",
                  "prices": [ { "stockCode": "005930", "rows": [
                    { "tradeDate": "20261016", "openPrice": 100, "closePrice": 105 } ] } ],
                  "errors": [ { "stockCode": "000660", "reason": "조회 실패" } ] }
                """);

        assertThat(krxService.getDailyPricesByRange(FROM, TO, List.of("005930", "000660")).getPrices()).hasSize(1);
    }

    /** Lambda 요청 형식은 yyyyMMdd다. */
    @Test
    void 날짜를_yyyyMMdd로_보낸다() throws Exception {
        respond("""
                { "from": "20230901", "to": "20261016",
                  "prices": [ { "stockCode": "005930", "rows": [
                    { "tradeDate": "20261016", "openPrice": 100, "closePrice": 105 } ] } ] }
                """);

        krxService.getDailyPricesByRange(FROM, TO, List.of("005930"));

        Mockito.verify(client).invokeGetDailyPricesByRange("20230901", "20261016", List.of("005930"));
    }

    /** 요청 종목이 모두 장기 거래정지라 prices가 비어도, skipped가 있으면 정상 응답이다. */
    @Test
    void prices가_비어도_skipped가_있으면_통과한다() throws Exception {
        respond("""
                { "from": "20230901", "to": "20261016",
                  "prices": [], "skipped": [ { "stockCode": "005930", "reason": "해당 기간 일봉 없음" } ] }
                """);

        assertThat(krxService.getDailyPricesByRange(FROM, TO, List.of("005930")).getSkipped()).hasSize(1);
    }

    // ---------- 날짜 모드 ----------

    @Test
    void 날짜_모드는_yyyyMMdd로_보낸다() throws Exception {
        Mockito.when(client.invokeGetDailyPricesByDate(anyString(), anyList()))
                .thenReturn(byDate("{ \"date\": \"20261016\", \"prices\": [] }"));

        krxService.getDailyPricesByDate(TO, List.of("005930"));

        Mockito.verify(client).invokeGetDailyPricesByDate("20261016", List.of("005930"));
    }

    /** 휴장일이나 전부 거래정지인 날은 prices가 비는 것이 정상이라 막지 않는다. 빠진 종목은 호출자가 실패로 기록한다. */
    @Test
    void 날짜_모드는_prices가_비어도_통과한다() throws Exception {
        Mockito.when(client.invokeGetDailyPricesByDate(anyString(), anyList()))
                .thenReturn(byDate("{ \"date\": \"20261016\", \"marketClosed\": true, \"prices\": [] }"));

        assertThat(krxService.getDailyPricesByDate(TO, List.of("005930")).isMarketClosed()).isTrue();
    }

    @Test
    void 날짜_모드도_응답이_null이면_실패다() {
        Mockito.when(client.invokeGetDailyPricesByDate(anyString(), anyList())).thenReturn(null);

        assertThatThrownBy(() -> krxService.getDailyPricesByDate(TO, List.of("005930")))
                .isInstanceOf(GeneralException.class)
                .extracting(e -> ((GeneralException) e).getErrorReasonHttpStatus().getCode())
                .isEqualTo(KrxErrorStatus.KRX_LAMBDA_RESPONSE_EMPTY.getCode());
    }

    private KrxDailyPricesByDateResponseDto byDate(String json) throws Exception {
        return objectMapper.readValue(json, KrxDailyPricesByDateResponseDto.class);
    }

    private void assertMismatch(Runnable call) {
        assertThatThrownBy(call::run)
                .isInstanceOf(GeneralException.class)
                .extracting(e -> ((GeneralException) e).getErrorReasonHttpStatus().getCode())
                .isEqualTo(KrxErrorStatus.KRX_LAMBDA_RESPONSE_MISMATCH.getCode());
    }

    // ---------- 응답이 요청과 맞는지 ----------

    @Test
    void 기간_모드_응답의_기간이_요청과_다르면_실패다() throws Exception {
        respond("""
                { "from": "20230901", "to": "20261015",
                  "prices": [ { "stockCode": "005930", "rows": [
                    { "tradeDate": "20261015", "openPrice": 100, "closePrice": 105 } ] } ] }
                """);

        assertMismatch(() -> krxService.getDailyPricesByRange(FROM, TO, List.of("005930")));
    }

    /** 날짜 모드 응답의 행에는 거래일이 없다. 다른 날짜의 시세가 요청 날짜로 저장되면 안 된다. */
    @Test
    void 날짜_모드_응답의_날짜가_요청과_다르면_실패다() throws Exception {
        Mockito.when(client.invokeGetDailyPricesByDate(anyString(), anyList()))
                .thenReturn(byDate("{ \"date\": \"20261015\", \"prices\": [] }"));

        assertMismatch(() -> krxService.getDailyPricesByDate(TO, List.of("005930")));
    }

    @Test
    void 날짜_모드_응답에_날짜가_없으면_실패다() throws Exception {
        Mockito.when(client.invokeGetDailyPricesByDate(anyString(), anyList()))
                .thenReturn(byDate("{ \"prices\": [] }"));

        assertMismatch(() -> krxService.getDailyPricesByDate(TO, List.of("005930")));
    }

    @Test
    void 휴장일이라면서_시세가_있으면_실패다() throws Exception {
        Mockito.when(client.invokeGetDailyPricesByDate(anyString(), anyList())).thenReturn(byDate("""
                { "date": "20261016", "marketClosed": true,
                  "prices": [ { "stockCode": "005930", "openPrice": 100, "closePrice": 105 } ] }
                """));

        assertMismatch(() -> krxService.getDailyPricesByDate(TO, List.of("005930")));
    }
}
