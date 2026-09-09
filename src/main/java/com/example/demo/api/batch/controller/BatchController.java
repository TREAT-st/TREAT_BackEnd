package com.example.demo.api.batch.controller;

import com.example.demo.api.batch.dto.BatchResponseDto.BatchExecutionListResponse;
import com.example.demo.api.batch.dto.BatchResponseDto.DailyBatchTriggerResponse;
import com.example.demo.api.batch.mapper.BatchConverter;
import com.example.demo.api.batch.service.BatchUseCase;
import com.example.demo.api.common.dto.ApiResponseDto;
import com.example.demo.domain.batch.exception.BatchHandler;
import com.example.demo.domain.batch.service.BatchExecutionLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;

import static com.example.demo.common.consts.StaticVariable.BATCH_SECRET_HEADER;
import static com.example.demo.common.consts.StaticVariable.SEOUL_ZONE;

@Tag(name = "[자동화] 배치 - 일일 자동화 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/batch")
@RequiredArgsConstructor
public class BatchController {

    private final BatchUseCase batchUseCase;
    private final BatchExecutionLogService batchExecutionLogService;

    /**
     * 리포트 콜백 시크릿과 다른 값이다.
     * 콜백 시크릿은 S3 이벤트 Lambda가 쓰고 이건 배치 트리거가 쓴다. 하나가 새도 다른 하나는 살아 있어야 한다.
     */
    @Value("${batch.trigger.secret}")
    private String triggerSecret;

    @Operation(summary = "일일 배치 실행 요청", description =
            "코스피200 동기화 → 변동성 탐지 → 리포트 생성 요청을 순서대로 실행합니다.<br>" +
                    "전체가 수 분 걸리므로 접수만 하고 202로 즉시 응답합니다. 진행 상황은 실행 이력 조회로 확인하세요.<br>" +
                    "이미 배치가 돌고 있으면 새로 접수하지 않고 accepted=false로 알립니다. 실패가 아니므로 재시도하지 마세요.<br>" +
                    "accepted=true는 비동기 실행기에 제출됐다는 뜻입니다. 각 단계가 실제로 돌았는지는 실행 이력에서 확인하세요.<br>" +
                    "force=true는 이미 성공한 단계도 다시 실행합니다. 리포트 생성 비용이 다시 발생하므로 주의하세요.<br>" +
                    "X-Batch-Secret 헤더에 BATCH_TRIGGER_SECRET 환경 변수 값을 넣어주세요.")
    @PostMapping("/daily-executions")
    public ResponseEntity<ApiResponseDto<DailyBatchTriggerResponse>> triggerDailyExecution(
            @RequestHeader(value = BATCH_SECRET_HEADER, required = false) String secret,
            @RequestParam(defaultValue = "false") boolean force) {
        verifyTriggerSecret(secret);

        try {
            batchUseCase.runDailyBatchAsync(force);
        } catch (TaskRejectedException e) {
            // 스레드가 하나뿐이고 큐를 두지 않아, 이미 배치가 돌고 있으면 여기서 즉시 거부된다.
            //
            // 이걸 4xx로 돌려주면 안 된다. 두 가지가 걸린다.
            // 하나는 자동화 계층이 실패로 보고 재시도해 헛도는 것,
            // 다른 하나는 인스턴스가 둘 이상일 때 두 번째 서버는 제출에 성공해 202가 나가고
            // 실행 이력의 IN_PROGRESS가 체인을 멈춘다는 것이다. 같은 상황에서 배포 구조에 따라
            // 응답이 달라지면 계약이 아니다. 접수 여부는 본문으로 알린다.
            log.info("이미 실행 중이라 접수하지 않았습니다.");
            return ResponseEntity.accepted()
                    .body(ApiResponseDto.onSuccess(BatchConverter.toAlreadyRunningResponse()));
        }

        return ResponseEntity.accepted()
                .body(ApiResponseDto.onSuccess(BatchConverter.toAcceptedResponse()));
    }

    @Operation(summary = "일일 배치 실행 상태 조회", description =
            "해당 거래일의 단계별 실행 상태를 실행 순서대로 조회합니다. 날짜 형식은 \"yyyy-MM-dd\"입니다.<br>" +
                    "tradeDate를 비우면 오늘 기준으로 조회합니다. 실행 요청 직후에는 거래일을 알 수 없으므로 비워서 부르면 됩니다.<br>" +
                    "VERIFY가 RUNNING인 것은 정상입니다. 리포트 도착 확인은 콜백이 도착해야 끝납니다.<br>" +
                    "휴장일이면 SYNC가 SKIPPED로, KRX 조회 자체가 실패했으면 SYNC가 FAILED로 남습니다.<br>" +
                    "steps가 비어 있으면 아직 실행 이력이 만들어지지 않은 상태입니다. " +
                    "한 번도 실행되지 않았거나, 접수 직후 KRX 조회가 진행 중일 수 있습니다.")
    @GetMapping("/daily-executions")
    public ApiResponseDto<BatchExecutionListResponse> getDailyExecutions(
            @RequestHeader(value = BATCH_SECRET_HEADER, required = false) String secret,
            @RequestParam(required = false) LocalDate tradeDate) {
        verifyTriggerSecret(secret);

        // 실행 요청은 거래일을 모른 채 접수된다(KRX가 알려주기 전이다).
        // 그래서 호출자가 조회 시점에도 날짜를 모를 수 있어, 비우면 오늘로 본다.
        LocalDate target = tradeDate != null ? tradeDate : LocalDate.now(SEOUL_ZONE);

        return ApiResponseDto.onSuccess(BatchConverter.toBatchExecutionListResponse(
                target, batchExecutionLogService.getByTradeDate(target)));
    }

    /** 길이가 달라도 시간이 새지 않도록 상수시간 비교를 쓴다. */
    private void verifyTriggerSecret(String secret) {
        if (secret == null || !MessageDigest.isEqual(
                secret.getBytes(StandardCharsets.UTF_8),
                triggerSecret.getBytes(StandardCharsets.UTF_8))) {
            throw BatchHandler.triggerUnauthorized();
        }
    }
}
