package com.example.demo.api.prediction.controller;

import com.example.demo.api.common.dto.ApiResponseDto;
import com.example.demo.api.prediction.dto.PredictionRequestDto.CreatePrediction;
import com.example.demo.api.prediction.dto.PredictionRequestDto.GradePrediction;
import com.example.demo.api.prediction.dto.PredictionResponseDto.CreatePredictionResult;
import com.example.demo.api.prediction.dto.PredictionResponseDto.PredictionResult;
import com.example.demo.api.prediction.service.PredictionUseCase;
import com.example.demo.common.annotation.AuthUser;
import com.example.demo.domain.user.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "[예측] 주가 예측 게임 API.")
@Validated
@RestController
@RequestMapping("/api/v1/predictions")
@RequiredArgsConstructor
public class PredictionController {

    private final PredictionUseCase predictionUseCase;

    @Operation(summary = "주가 예측 제출",
            description = "종목/예측기간/상승폭을 받아 예측을 등록하고 획득 가능 포인트를 반환합니다. (결과는 만기 후 채점)")
    @PostMapping
    public ApiResponseDto<CreatePredictionResult> createPrediction(
            @AuthUser User user,
            @Valid @RequestBody CreatePrediction request) {
        return ApiResponseDto.onSuccess(predictionUseCase.createPrediction(user, request));
    }

    @Operation(summary = "예측 결과 조회",
            description = "예측 결과(적중/실패/대기)와 실제 등락률, 원인을 반환합니다.")
    @GetMapping("/{predictionId}/result")
    public ApiResponseDto<PredictionResult> getResult(
            @AuthUser User user,
            @PathVariable Long predictionId) {
        return ApiResponseDto.onSuccess(predictionUseCase.getResult(user, predictionId));
    }

    @Operation(summary = "[데모/테스트] 수동 채점",
            description = "만기 종가를 직접 넣어 즉시 채점합니다. 실서비스는 스케줄러가 자동 처리하므로 데모용입니다.")
    @PostMapping("/{predictionId}/grade")
    public ApiResponseDto<PredictionResult> gradeManually(
            @AuthUser User user,
            @PathVariable Long predictionId,
            @Valid @RequestBody GradePrediction request) {
        return ApiResponseDto.onSuccess(
                predictionUseCase.gradeManually(user, predictionId, request.getMaturityPrice()));
    }
}
