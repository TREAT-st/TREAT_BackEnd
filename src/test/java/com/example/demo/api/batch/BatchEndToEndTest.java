package com.example.demo.api.batch;

import com.example.demo.api.krx.dto.KrxKospi200ResponseDto;
import com.example.demo.api.krx.service.KrxService;
import com.example.demo.api.report.client.ReportLambdaClient;
import com.example.demo.domain.batch.entity.BatchExecutionLog;
import com.example.demo.domain.batch.entity.BatchStatus;
import com.example.demo.domain.batch.entity.BatchStep;
import com.example.demo.domain.batch.repository.BatchExecutionLogRepository;
import com.example.demo.domain.stock.repository.StockRepository;
import com.example.demo.domain.volatility.entity.VolatilityDetectionResult;
import com.example.demo.domain.volatility.entity.VolatilitySignal;
import com.example.demo.domain.volatility.repository.VolatilityRepository;
import com.example.demo.domain.volatility.service.VolatilityDetectionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.Executor;

import static com.example.demo.common.consts.StaticVariable.BATCH_SECRET_HEADER;
import static com.example.demo.common.consts.StaticVariable.SEOUL_ZONE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 1(실행 이력) + Phase 2(체인) + Phase 3(엔드포인트)을 HTTP로 전부 관통한다.
 *
 * 유스케이스를 직접 부르는 통합 테스트와 달리 시큐리티 필터와 컨트롤러를 실제로 지나므로,
 * permitAll 설정이 빠지거나 헤더 이름이 어긋나는 것까지 잡힌다.
 *
 * 배치 실행기를 동기 실행기로 갈아끼운다. @Async 그대로 두면 응답이 먼저 돌아와
 * 검증과 실행이 경합한다.
 */
@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(BatchEndToEndTest.SyncBatchExecutorConfig.class)
class BatchEndToEndTest {

    private static final String SECRET = "test-batch-secret";
    private static final LocalDate TODAY = LocalDate.now(SEOUL_ZONE);
    private static final DateTimeFormatter TRADE_DATE_FORMAT = DateTimeFormatter.ofPattern("uuuuMMdd");

    @TestConfiguration
    static class SyncBatchExecutorConfig {
        @Bean("batchTaskExecutor")
        Executor batchTaskExecutor() {
            return new SyncTaskExecutor();
        }
    }

    @MockitoBean
    private KrxService krxService;
    @MockitoBean
    private VolatilityDetectionService volatilityDetectionService;
    @MockitoBean
    private ReportLambdaClient reportLambdaClient;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private BatchExecutionLogRepository batchExecutionLogRepository;
    @Autowired
    private VolatilityRepository volatilityRepository;
    @Autowired
    private StockRepository stockRepository;

    @BeforeEach
    void setUp() throws Exception {
        batchExecutionLogRepository.deleteAll();
        volatilityRepository.deleteAll();
        stockRepository.deleteAll();

        VolatilitySignal detected = signal();
        Mockito.when(krxService.getKospi200Prices()).thenReturn(kospi200Response());
        Mockito.when(volatilityDetectionService.detect(anyInt()))
                .thenReturn(new VolatilityDetectionResult(TODAY, 100, List.of(detected)));
        Mockito.when(volatilityDetectionService.selectTop(any(), anyInt(), anyInt()))
                .thenReturn(List.of(detected));
    }

    @Test
    void 트리거부터_이력조회까지_HTTP로_관통한다() throws Exception {
        mockMvc.perform(post("/api/v1/batch/daily-executions")
                        .header(BATCH_SECRET_HEADER, SECRET))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.isSuccess").value(true));

        // 체인이 실제로 돌아 네 단계가 남았다.
        assertThat(batchExecutionLogRepository.findAllByTradeDate(TODAY)).hasSize(4);
        assertThat(stockRepository.findAll()).hasSize(1);
        assertThat(volatilityRepository.findAllByTradeDateOrderByIdAsc(TODAY)).hasSize(1);
        Mockito.verify(reportLambdaClient).invokeCreateReport(any());

        mockMvc.perform(get("/api/v1/batch/daily-executions")
                        .header(BATCH_SECRET_HEADER, SECRET)
                        .param("tradeDate", TODAY.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.tradeDate").value(TODAY.toString()))
                // 실행 순서대로 나온다. DB 정렬은 알파벳순이라 서비스가 다시 정렬한다.
                .andExpect(jsonPath("$.result.steps[0].step").value("SYNC"))
                .andExpect(jsonPath("$.result.steps[0].status").value("SUCCESS"))
                .andExpect(jsonPath("$.result.steps[1].step").value("DETECT"))
                .andExpect(jsonPath("$.result.steps[1].status").value("SUCCESS"))
                .andExpect(jsonPath("$.result.steps[2].step").value("REPORT"))
                .andExpect(jsonPath("$.result.steps[2].status").value("SUCCESS"))
                // 도착 확인은 콜백이 와야 닫힌다.
                .andExpect(jsonPath("$.result.steps[3].step").value("VERIFY"))
                .andExpect(jsonPath("$.result.steps[3].status").value("RUNNING"))
                .andExpect(jsonPath("$.result.steps[3].attempt").value(1));
    }

    /** 재실행에서 앞 단계를 다시 돌리지 않는 것이 이 체인의 존재 이유다. */
    @Test
    void 같은_날_다시_트리거해도_리포트를_다시_요청하지_않는다() throws Exception {
        mockMvc.perform(post("/api/v1/batch/daily-executions").header(BATCH_SECRET_HEADER, SECRET))
                .andExpect(status().isAccepted());
        Mockito.clearInvocations(volatilityDetectionService, reportLambdaClient);

        mockMvc.perform(post("/api/v1/batch/daily-executions").header(BATCH_SECRET_HEADER, SECRET))
                .andExpect(status().isAccepted());

        Mockito.verify(volatilityDetectionService, Mockito.never()).detect(anyInt());
        Mockito.verify(reportLambdaClient, Mockito.never()).invokeCreateReport(any());
        assertThat(batchExecutionLogRepository.findByTradeDateAndStep(TODAY, BatchStep.REPORT).orElseThrow()
                .getAttempt()).isEqualTo(1);
    }

    /**
     * 부분 재개가 이 체인의 존재 이유다. 실패한 단계만 다시 돌고 앞 단계는 그대로 둔다.
     * 같은 행을 재사용하므로 회차만 오른다.
     */
    @Test
    void DETECT가_실패하면_다음_실행이_그_단계부터_이어간다() throws Exception {
        Mockito.doThrow(new IllegalStateException("KRX OHLCV 조회 실패"))
                .when(volatilityDetectionService).detect(anyInt());

        mockMvc.perform(post("/api/v1/batch/daily-executions").header(BATCH_SECRET_HEADER, SECRET))
                .andExpect(status().isAccepted());

        assertThat(statusOf(BatchStep.SYNC)).isEqualTo(BatchStatus.SUCCESS);
        assertThat(statusOf(BatchStep.DETECT)).isEqualTo(BatchStatus.FAILED);
        assertThat(logOf(BatchStep.DETECT).getMessage()).isEqualTo("KRX OHLCV 조회 실패");
        // 뒷 단계는 시작조차 하지 않는다.
        assertThat(batchExecutionLogRepository.findByTradeDateAndStep(TODAY, BatchStep.REPORT)).isEmpty();
        Mockito.verify(reportLambdaClient, Mockito.never()).invokeCreateReport(any());

        Mockito.doReturn(new VolatilityDetectionResult(TODAY, 100, List.of(signal())))
                .when(volatilityDetectionService).detect(anyInt());

        mockMvc.perform(post("/api/v1/batch/daily-executions").header(BATCH_SECRET_HEADER, SECRET))
                .andExpect(status().isAccepted());

        // SYNC는 다시 돌지 않았다.
        assertThat(logOf(BatchStep.SYNC).getAttempt()).isEqualTo(1);
        // DETECT는 같은 행에서 회차만 올랐다.
        assertThat(logOf(BatchStep.DETECT).getAttempt()).isEqualTo(2);
        assertThat(statusOf(BatchStep.DETECT)).isEqualTo(BatchStatus.SUCCESS);
        assertThat(logOf(BatchStep.DETECT).getFinishedAt()).isNotNull();

        assertThat(statusOf(BatchStep.REPORT)).isEqualTo(BatchStatus.SUCCESS);
        assertThat(statusOf(BatchStep.VERIFY)).isEqualTo(BatchStatus.RUNNING);
    }

    /** force는 성공한 단계도 되돌린다. 리포트 비용이 다시 나가므로 시크릿 뒤에 있어야 한다. */
    @Test
    void force면_리포트를_다시_요청한다() throws Exception {
        mockMvc.perform(post("/api/v1/batch/daily-executions").header(BATCH_SECRET_HEADER, SECRET))
                .andExpect(status().isAccepted());
        Mockito.clearInvocations(reportLambdaClient);

        mockMvc.perform(post("/api/v1/batch/daily-executions")
                        .header(BATCH_SECRET_HEADER, SECRET)
                        .param("force", "true"))
                .andExpect(status().isAccepted());

        Mockito.verify(reportLambdaClient).invokeCreateReport(any());
        assertThat(batchExecutionLogRepository.findByTradeDateAndStep(TODAY, BatchStep.REPORT).orElseThrow()
                .getAttempt()).isEqualTo(2);
    }

    /**
     * 시큐리티는 이 경로를 통과시키고(permitAll) 인증은 컨트롤러가 헤더로 한다.
     * permitAll이 빠지면 401이 아니라 시큐리티 단에서 막혀 이 검증이 깨진다.
     */
    @Test
    void 시크릿이_틀리면_401이고_배치는_돌지_않는다() throws Exception {
        mockMvc.perform(post("/api/v1/batch/daily-executions")
                        .header(BATCH_SECRET_HEADER, "wrong-secret"))
                .andExpect(status().isUnauthorized());

        assertThat(batchExecutionLogRepository.findAll()).isEmpty();
        Mockito.verify(krxService, Mockito.never()).getKospi200Prices();
    }

    @Test
    void 시크릿_헤더가_없어도_401이다() throws Exception {
        mockMvc.perform(post("/api/v1/batch/daily-executions"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 이력_조회도_시크릿을_요구한다() throws Exception {
        mockMvc.perform(get("/api/v1/batch/daily-executions")
                        .param("tradeDate", TODAY.toString()))
                .andExpect(status().isUnauthorized());
    }

    /**
     * 휴장일과 KRX 장애는 둘 다 체인을 시작조차 못 하지만 원인이 완전히 다르다.
     * 아무것도 안 남기면 조회에서 구분되지 않아 운영자가 원인을 알 수 없다.
     */
    @Test
    void 휴장일은_실행일에_스킵으로_남는다() throws Exception {
        LocalDate krxTradeDate = TODAY.minusDays(3);
        Mockito.when(krxService.getKospi200Prices()).thenReturn(kospi200Response(krxTradeDate));

        mockMvc.perform(post("/api/v1/batch/daily-executions").header(BATCH_SECRET_HEADER, SECRET))
                .andExpect(status().isAccepted());

        Mockito.verify(volatilityDetectionService, Mockito.never()).detect(anyInt());
        // KRX가 준 거래일의 기록은 건드리지 않는다.
        assertThat(batchExecutionLogRepository.findAllByTradeDate(krxTradeDate)).isEmpty();

        mockMvc.perform(get("/api/v1/batch/daily-executions").header(BATCH_SECRET_HEADER, SECRET))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.steps[0].step").value("SYNC"))
                .andExpect(jsonPath("$.result.steps[0].status").value("SKIPPED"))
                .andExpect(jsonPath("$.result.steps[0].message").value(
                        "거래일이 아닙니다. KRX 거래일=" + krxTradeDate));
    }

    @Test
    void KRX_장애는_실행일에_실패로_남는다() throws Exception {
        Mockito.doThrow(new IllegalStateException("KRX Lambda 호출 실패"))
                .when(krxService).getKospi200Prices();

        mockMvc.perform(post("/api/v1/batch/daily-executions").header(BATCH_SECRET_HEADER, SECRET))
                .andExpect(status().isAccepted());

        mockMvc.perform(get("/api/v1/batch/daily-executions").header(BATCH_SECRET_HEADER, SECRET))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.steps[0].step").value("SYNC"))
                .andExpect(jsonPath("$.result.steps[0].status").value("FAILED"))
                .andExpect(jsonPath("$.result.steps[0].message").value("KRX Lambda 호출 실패"));
    }

    /** 실행일이 실제 거래일이었다면, KRX 복구 후 재실행이 같은 키를 이어받아야 한다. */
    @Test
    void KRX가_복구되면_같은_키를_이어받아_완주한다() throws Exception {
        Mockito.doThrow(new IllegalStateException("KRX Lambda 호출 실패"))
                .when(krxService).getKospi200Prices();
        mockMvc.perform(post("/api/v1/batch/daily-executions").header(BATCH_SECRET_HEADER, SECRET))
                .andExpect(status().isAccepted());
        assertThat(statusOf(BatchStep.SYNC)).isEqualTo(BatchStatus.FAILED);

        Mockito.doReturn(kospi200Response()).when(krxService).getKospi200Prices();
        mockMvc.perform(post("/api/v1/batch/daily-executions").header(BATCH_SECRET_HEADER, SECRET))
                .andExpect(status().isAccepted());

        assertThat(statusOf(BatchStep.SYNC)).isEqualTo(BatchStatus.SUCCESS);
        assertThat(logOf(BatchStep.SYNC).getAttempt()).isEqualTo(2);
        assertThat(statusOf(BatchStep.VERIFY)).isEqualTo(BatchStatus.RUNNING);
    }

    private BatchStatus statusOf(BatchStep step) {
        return logOf(step).getStatus();
    }

    private BatchExecutionLog logOf(BatchStep step) {
        return batchExecutionLogRepository.findByTradeDateAndStep(TODAY, step).orElseThrow();
    }

    private KrxKospi200ResponseDto kospi200Response() throws Exception {
        return kospi200Response(TODAY);
    }

    /** DTO에 세터가 없어 역직렬화로 만든다. */
    private KrxKospi200ResponseDto kospi200Response(LocalDate tradeDate) throws Exception {
        String json = ("{\"tradeDate\":\"%s\",\"requestedCount\":1,"
                + "\"stocks\":[{\"stockCode\":\"005930\",\"stockName\":\"삼성전자\","
                + "\"openPrice\":70000,\"closePrice\":71000}],"
                + "\"errors\":[]}").formatted(tradeDate.format(TRADE_DATE_FORMAT));
        return new ObjectMapper().readValue(json, KrxKospi200ResponseDto.class);
    }

    private VolatilitySignal signal() {
        return new VolatilitySignal("005930", "삼성전자", 1,
                6.0, 7.0, 30.0, 0.95, 10.0, 3.0, 0.85, 1.0, true, List.of("daily_return"));
    }
}
