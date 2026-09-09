package com.example.demo.api.prediction.controller;

import com.example.demo.api.common.dto.ApiResponseDto;
import com.example.demo.api.prediction.dto.PredictionRequestDto.SubmitPredictionRequest;
import com.example.demo.api.prediction.dto.PredictionResponseDto.PredictionResultResponse;
import com.example.demo.api.prediction.dto.PredictionResponseDto.SubmitPredictionResponse;
import com.example.demo.api.prediction.service.PredictionUseCase;
import com.example.demo.common.annotation.AuthUser;
import com.example.demo.domain.user.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "[주가 예측 게임] 예측 API")
@RestController
@RequestMapping("/api/v1/predictions")
@RequiredArgsConstructor
public class PredictionController {

    private final PredictionUseCase predictionUseCase;

    @Operation(summary = "예측 제출",
            description = "종목 코드, 예측 기간(duration), 변동 구간(target)을 입력해 예측을 제출합니다.<br>" +
                    "기준가(현재가)가 저장되며 획득 가능 포인트를 반환합니다.<br>" +
                    "duration: ONE_DAY / THREE_DAY / FIVE_DAY / ONE_WEEK / TWO_WEEK<br>" +
                    "target: UNDER_3(3% 미만) / BETWEEN_3_5(3~5%) / OVER_5(5% 이상)")
    @PostMapping
    public ApiResponseDto<SubmitPredictionResponse> submitPrediction(
            @AuthUser User user,
            @RequestBody @Valid SubmitPredictionRequest request) {
        return ApiResponseDto.onSuccess(predictionUseCase.submitPrediction(user, request));
    }

    @Operation(summary = "예측 결과 조회",
            description = "예측 ID로 예측 결과를 조회합니다.<br>" +
                    "status: PENDING(채점 전) / CORRECT(적중) / WRONG(미적중)")
    @GetMapping("/{predictionId}/result")
    public ApiResponseDto<PredictionResultResponse> getPredictionResult(
            @AuthUser User user,
            @PathVariable Long predictionId) {
        return ApiResponseDto.onSuccess(predictionUseCase.getPredictionResult(predictionId));
    }

}