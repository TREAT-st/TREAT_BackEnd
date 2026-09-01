package com.example.demo.api.volatility.service;

import com.example.demo.api.report.client.ReportLambdaClient;
import com.example.demo.api.report.dto.ReportLambdaRequestDto;
import com.example.demo.common.exception.GeneralException;
import com.example.demo.domain.volatility.entity.Volatility;
import com.example.demo.domain.volatility.entity.VolatilityDetectionResult;
import com.example.demo.domain.volatility.entity.VolatilitySignal;
import com.example.demo.domain.volatility.exception.VolatilityErrorStatus;
import com.example.demo.domain.volatility.service.VolatilityCommandService;
import com.example.demo.domain.volatility.service.VolatilityDetectionService;
import com.example.demo.domain.volatility.service.VolatilityQueryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.util.List;

import static com.example.demo.api.volatility.dto.VolatilityRequestDto.ReportCallback;
import static com.example.demo.api.volatility.dto.VolatilityRequestDto.SingleReportRequest;
import static com.example.demo.api.volatility.dto.VolatilityRequestDto.ReportGenerationRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * runDetection의 저장 가드와, 거래일·유니버스 크기가 KRX 응답에서 그대로 전달되는지 검증.
 * detect()는 종목별 실패를 스킵하므로 전 종목이 실패해도 빈 리스트를 정상 반환한다.
 * 그 상태로 저장까지 진행하면 안 되므로, 저장 이전에 중단되는지를 확인한다.
 */
class VolatilityUseCaseTest {

    private static final LocalDate TRADE_DATE = LocalDate.of(2026, 8, 14);

    private final VolatilityQueryService queryService = Mockito.mock(VolatilityQueryService.class);
    private final VolatilityDetectionService detectionService = Mockito.mock(VolatilityDetectionService.class);
    private final VolatilityCommandService commandService = Mockito.mock(VolatilityCommandService.class);
    private final ReportLambdaClient reportLambdaClient = Mockito.mock(ReportLambdaClient.class);

    private final VolatilityUseCase useCase =
            new VolatilityUseCase(queryService, detectionService, commandService, reportLambdaClient);

    @Test
    void 전_종목_분석에_실패하면_저장하지_않고_예외를_던진다() {
        Mockito.when(detectionService.detect(anyInt())).thenReturn(detection(List.of()));

        assertThatThrownBy(useCase::runDetection)
                .isInstanceOf(GeneralException.class)
                .extracting(e -> ((GeneralException) e).getErrorReasonHttpStatus().getCode())
                .isEqualTo(VolatilityErrorStatus.VOLATILITY_DETECTION_FAILED.getCode());

        // 저장 경로에 진입하면 안 된다.
        verify(commandService, never()).saveTopVolatilityStocks(any(), any());
    }

    @Test
    void 분석은_됐지만_알림_종목이_없으면_빈_결과로_저장한다() {
        Mockito.when(detectionService.detect(anyInt())).thenReturn(detection(List.of(signal("005930"))));
        Mockito.when(detectionService.selectTop(any(), anyInt(), anyInt())).thenReturn(List.of());

        var result = useCase.runDetection();

        assertThat(result.getDetectedCount()).isZero();
        verify(commandService).saveTopVolatilityStocks(List.of(), TRADE_DATE);
    }

    @Test
    void 알림_종목이_있으면_그대로_저장한다() {
        VolatilitySignal detected = signal("005930");
        Mockito.when(detectionService.detect(anyInt())).thenReturn(detection(List.of(detected)));
        Mockito.when(detectionService.selectTop(any(), anyInt(), anyInt())).thenReturn(List.of(detected));

        var result = useCase.runDetection();

        assertThat(result.getDetectedCount()).isEqualTo(1);
        assertThat(result.getStocks()).singleElement()
                .satisfies(s -> assertThat(s.getStockCode()).isEqualTo("005930"));
        verify(commandService).saveTopVolatilityStocks(List.of(detected), TRADE_DATE);
    }

    @Test
    void 저장되는_거래일은_KRX_응답의_거래일을_따른다() {
        VolatilitySignal detected = signal("005930");
        LocalDate krxTradeDate = LocalDate.of(2026, 8, 11);
        Mockito.when(detectionService.detect(anyInt()))
                .thenReturn(new VolatilityDetectionResult(krxTradeDate, 100, List.of(detected)));
        Mockito.when(detectionService.selectTop(any(), anyInt(), anyInt())).thenReturn(List.of(detected));

        var result = useCase.runDetection();

        assertThat(result.getTradeDate()).isEqualTo(krxTradeDate);
        verify(commandService).saveTopVolatilityStocks(List.of(detected), krxTradeDate);
    }

    /**
     * 시총 가중치의 분모를 요청 수(100)로 하드코딩하면, Lambda가 그보다 적게 보냈을 때 순위가 왜곡된다.
     * 응답의 universe_size가 그대로 selectTop에 전달돼야 한다.
     */
    @Test
    void 시총_가중치_분모는_응답의_universeSize를_사용한다() {
        VolatilitySignal detected = signal("005930");
        Mockito.when(detectionService.detect(anyInt()))
                .thenReturn(new VolatilityDetectionResult(TRADE_DATE, 87, List.of(detected)));
        Mockito.when(detectionService.selectTop(any(), anyInt(), anyInt())).thenReturn(List.of(detected));

        useCase.runDetection();

        verify(detectionService).selectTop(List.of(detected), 87, 10);
    }

    /**
     * Lambda의 validate_job은 gptModel 키가 있으면 비어 있지 않은 문자열이길 요구한다.
     * 빈 문자열은 @JsonInclude(NON_NULL)에 걸리지 않아 그대로 전송되고 Lambda 실행 오류가 된다.
     * Swagger UI에서 선택 입력값을 비우고 보내면 실제로 이 경로를 탄다.
     */
    @Test
    void gptModel이_공백이면_페이로드에서_제외된다() throws Exception {
        Mockito.when(queryService.getLatestVolatility()).thenReturn(List.of(volatility()));

        useCase.runReportGeneration(reportRequest("{\"gptModel\":\"   \"}"));

        assertThat(capturedReportJob().getGptModel()).isNull();
    }

    @Test
    void gptModel이_있으면_공백을_제거해_전달한다() throws Exception {
        Mockito.when(queryService.getLatestVolatility()).thenReturn(List.of(volatility()));

        useCase.runReportGeneration(reportRequest("{\"gptModel\":\" gpt-5-mini \"}"));

        assertThat(capturedReportJob().getGptModel()).isEqualTo("gpt-5-mini");
    }

    /**
     * 콜백은 reportDate를 그대로 되돌려 (stockCode, tradeDate)로 행을 찾는다.
     * 서버 날짜를 보내면 휴장일이나 과거 탐지 결과 재생성에서 행을 못 찾아
     * 리포트는 만들어졌는데 reportUrl이 비는 상태가 된다.
     */
    @Test
    void 단일_리포트도_저장된_거래일을_사용한다() throws Exception {
        LocalDate storedTradeDate = LocalDate.of(2026, 8, 11);
        Mockito.when(queryService.getLatestByStockCode("005930"))
                .thenReturn(Volatility.builder()
                        .stockCode("005930").stockName("삼성전자").tradeDate(storedTradeDate).build());

        useCase.runSingleReportGeneration(new ObjectMapper().readValue(
                "{\"stockCode\":\"005930\",\"stockName\":\"삼성전자\"}", SingleReportRequest.class));

        ReportLambdaRequestDto job = capturedReportJob();
        assertThat(job.getReportDate()).isEqualTo("20260811");
        assertThat(job.getJobId()).isEqualTo("VOLATILITY_20260811_005930");
    }

    /**
     * yyyyMMdd의 기본 SMART 해석은 20260231을 2월 말로 보정한다.
     * 그대로 두면 콜백이 존재하지 않는 날짜로 엉뚱한 행을 갱신할 수 있다.
     */
    @Test
    void 콜백의_존재하지_않는_날짜는_거부한다() throws Exception {
        assertThatThrownBy(() -> useCase.handleReportCallback(new ObjectMapper().readValue(
                "{\"stockCode\":\"005930\",\"reportDate\":\"20260231\","
                        + "\"reportUrl\":\"https://example.com/x_analysis.html\"}", ReportCallback.class)))
                .isInstanceOf(GeneralException.class)
                .extracting(e -> ((GeneralException) e).getErrorReasonHttpStatus().getCode())
                .isEqualTo(VolatilityErrorStatus.REPORT_CALLBACK_INVALID_REQUEST.getCode());

        verify(commandService, never()).updateReportUrl(any(), any(), any());
    }

    private ReportLambdaRequestDto capturedReportJob() {
        ArgumentCaptor<ReportLambdaRequestDto> captor = ArgumentCaptor.forClass(ReportLambdaRequestDto.class);
        verify(reportLambdaClient).invokeCreateReport(captor.capture());
        return captor.getValue();
    }

    private ReportGenerationRequest reportRequest(String json) throws Exception {
        return new ObjectMapper().readValue(json, ReportGenerationRequest.class);
    }

    private Volatility volatility() {
        return Volatility.builder()
                .stockCode("005930")
                .stockName("삼성전자")
                .tradeDate(TRADE_DATE)
                .build();
    }

    private VolatilityDetectionResult detection(List<VolatilitySignal> signals) {
        return new VolatilityDetectionResult(TRADE_DATE, 100, signals);
    }

    private VolatilitySignal signal(String stockCode) {
        return new VolatilitySignal(
                stockCode, "삼성전자", 1,
                0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                0.5, true, List.of()
        );
    }
}
