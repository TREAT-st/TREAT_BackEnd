package com.example.demo.api.prediction.controller;

import com.example.demo.api.common.dto.ApiResponseDto;
import com.example.demo.api.prediction.dto.PredictionResponseDto.GradePredictionResponse;
import com.example.demo.api.prediction.dto.PredictionResponseDto.SchedulerRunResponse;
import com.example.demo.api.prediction.service.PredictionUseCase;
import com.example.demo.common.annotation.AuthUser;
import com.example.demo.domain.user.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "[더미데이터/동작확인]")
@RestController
@RequestMapping("/api/v1/predictions")
@RequiredArgsConstructor
public class PredictionTestController {

    private final PredictionUseCase predictionUseCase;

    @Operation(summary = "단건 수동 채점",
            description = "예측 ID로 단건 채점을 즉시 실행합니다.<br>" +
                    "PENDING 상태인 예측만 채점 가능하며, 동작 확인 목적의 API입니다.<br>" +
                    "실제 채점은 만기일(maturityAt) 이후 스케줄러가 자동으로 수행합니다.")
    @PostMapping("/{predictionId}/grade")
    public ApiResponseDto<GradePredictionResponse> manualGrade(
            @AuthUser User user,
            @PathVariable Long predictionId) {
        return ApiResponseDto.onSuccess(predictionUseCase.manualGrade(predictionId));
    }

    @Operation(summary = "채점 스케줄러 수동 트리거",
            description = "만기 도래한 PENDING 예측을 일괄 채점하는 스케줄러를 수동으로 실행합니다.<br>" +
                    "평일 16:30(KST)에 자동 실행되며, 이 API는 동작 확인 목적입니다.")
    @PostMapping("/scheduler/run")
    public ApiResponseDto<SchedulerRunResponse> runScheduler(@AuthUser User user) {
        return ApiResponseDto.onSuccess(predictionUseCase.gradeMaturedPredictions());
    }
}