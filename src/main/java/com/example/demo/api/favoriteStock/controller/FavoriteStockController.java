package com.example.demo.api.favoriteStock.controller;

import com.example.demo.api.common.dto.ApiResponseDto;
import com.example.demo.api.favoriteStock.dto.FavoriteStockRequestDto;
import com.example.demo.api.favoriteStock.dto.FavoriteStockResponseDto.*;
import com.example.demo.api.favoriteStock.service.FavoriteStockUseCase;
import com.example.demo.common.annotation.AuthUser;
import com.example.demo.domain.user.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@Tag(name = "[사용자 관심 종목 페이지] 사용자 관심 종목 API")
@RestController
@RequestMapping("/api/v1/users/favorite-stocks")
@RequiredArgsConstructor
public class FavoriteStockController {
    private final FavoriteStockUseCase favoriteStockUseCase;

    @Operation(summary = "user의 관심 종목 등록", description = "user의 관심 종목에 해당 종목을 등록합니다.")
    @PostMapping
    public ApiResponseDto<Long> addUserFavoriteStock(
            @AuthUser User user,
            @RequestBody @Valid FavoriteStockRequestDto favoriteStockRequest) {
        return ApiResponseDto.onSuccess(favoriteStockUseCase.addFavoriteStock(user, favoriteStockRequest));
    }

    @Operation(summary = "user의 관심 종목들 목록", description = "user의 관심 종목들을 pagination으로 조회합니다.<br>" +
            "각 종목에 최신 시세를 함께 내려줍니다. currentPrice는 실시간 시세가 아니라 tradeDate의 종가입니다.<br>" +
            "priceChange·changeRate는 직전 거래일 종가 대비 값이며 하락이면 음수입니다.<br>" +
            "시세 필드가 null인 경우: stock에 없는 종목이거나, 아직 한 번도 시세가 반영되지 않은 종목입니다.<br>" +
            "최근 동기화에서 시세를 못 받은 종목(거래정지 등)은 null이 아니라 마지막으로 반영된 값이 그대로 나오며, " +
            "tradeDate가 다른 종목보다 이전 날짜입니다.")
    @GetMapping
    public ApiResponseDto<FavoriteStockPageResponse> getFavoriteStockByPage(
            @AuthUser User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        Pageable pageable = PageRequest.of(page, pageSize,
                Sort.by(Sort.Order.desc("isAlertEnabled"), Sort.Order.asc("stockCode")));
        return ApiResponseDto.onSuccess(favoriteStockUseCase.getFavoriteStockPageByUserId(user.getId(), pageable));
    }

    @Operation(summary = "user의 관심 종목에서 종목 삭제", description = "user의 관심 종목에서 해당 종목을 삭제합니다.")
    @DeleteMapping("/{favoriteStockId}")
    public ApiResponseDto<Void> deleteUserFavoriteStock(
            @AuthUser User user,
            @PathVariable Long favoriteStockId) {
        favoriteStockUseCase.deleteFavoriteStock(user.getId(), favoriteStockId);

        return ApiResponseDto.onSuccess(null);
    }
}
