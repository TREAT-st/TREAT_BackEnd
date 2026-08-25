package com.example.demo.api.volatility.service;

import com.example.demo.api.report.client.ReportLambdaClient;
import com.example.demo.common.exception.GeneralException;
import com.example.demo.domain.volatility.entity.VolatilitySignal;
import com.example.demo.domain.volatility.exception.VolatilityErrorStatus;
import com.example.demo.domain.volatility.service.VolatilityCommandService;
import com.example.demo.domain.volatility.service.VolatilityDetectionService;
import com.example.demo.domain.volatility.service.VolatilityQueryService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * runDetection의 저장 가드 검증.
 * detect()는 종목별 실패를 스킵하므로 전 종목이 실패해도 빈 리스트를 정상 반환한다.
 * 그 상태로 저장까지 진행하면 saveTopVolatilityStocks가 오늘 기록을 지우기만 하고 끝나므로,
 * 저장 이전에 중단되는지를 확인한다.
 */
class VolatilityUseCaseTest {

    private final VolatilityQueryService queryService = Mockito.mock(VolatilityQueryService.class);
    private final VolatilityDetectionService detectionService = Mockito.mock(VolatilityDetectionService.class);
    private final VolatilityCommandService commandService = Mockito.mock(VolatilityCommandService.class);
    private final ReportLambdaClient reportLambdaClient = Mockito.mock(ReportLambdaClient.class);

    private final VolatilityUseCase useCase =
            new VolatilityUseCase(queryService, detectionService, commandService, reportLambdaClient);

    @Test
    void 전_종목_분석에_실패하면_저장하지_않고_예외를_던진다() {
        Mockito.when(detectionService.detect(anyInt())).thenReturn(List.of());

        assertThatThrownBy(useCase::runDetection)
                .isInstanceOf(GeneralException.class)
                .extracting(e -> ((GeneralException) e).getErrorReasonHttpStatus().getCode())
                .isEqualTo(VolatilityErrorStatus.VOLATILITY_DETECTION_FAILED.getCode());

        // 오늘 기록을 지우는 저장 경로에 진입하면 안 된다.
        verify(commandService, never()).saveTopVolatilityStocks(any());
    }

    @Test
    void 분석은_됐지만_알림_종목이_없으면_빈_결과로_저장한다() {
        Mockito.when(detectionService.detect(anyInt())).thenReturn(List.of(signal("005930")));
        Mockito.when(detectionService.selectTop(any(), anyInt(), anyInt())).thenReturn(List.of());

        var result = useCase.runDetection();

        assertThat(result.getDetectedCount()).isZero();
        verify(commandService).saveTopVolatilityStocks(List.of());
    }

    @Test
    void 알림_종목이_있으면_그대로_저장한다() {
        VolatilitySignal detected = signal("005930");
        Mockito.when(detectionService.detect(anyInt())).thenReturn(List.of(detected));
        Mockito.when(detectionService.selectTop(any(), anyInt(), anyInt())).thenReturn(List.of(detected));

        var result = useCase.runDetection();

        assertThat(result.getDetectedCount()).isEqualTo(1);
        assertThat(result.getStocks()).singleElement()
                .satisfies(s -> assertThat(s.getStockCode()).isEqualTo("005930"));
        verify(commandService).saveTopVolatilityStocks(List.of(detected));
    }

    private VolatilitySignal signal(String stockCode) {
        return new VolatilitySignal(
                stockCode, "삼성전자", 1,
                0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                0.5, true, List.of()
        );
    }
}
