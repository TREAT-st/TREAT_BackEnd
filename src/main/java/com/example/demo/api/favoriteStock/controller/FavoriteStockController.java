package com.example.demo.api.favoriteStock.controller;

import com.example.demo.api.common.dto.ApiResponseDto;
import com.example.demo.api.favoriteStock.dto.FavoriteStockRequestDto;
import com.example.demo.api.favoriteStock.dto.FavoriteStockResponseDto.*;
import com.example.demo.api.favoriteStock.mapper.FavoriteStockConverter;
import com.example.demo.api.favoriteStock.service.FavoriteStockUseCase;
import com.example.demo.domain.favoriteStock.entity.FavoriteStock;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@Tag(name = "[사용자 관심 종목 페이지] 사용자 관심 종목 API")
@RestController
@RequestMapping("/api/v1/users/{userId}/favorite-stocks")
@RequiredArgsConstructor
public class FavoriteStockController {
    private final FavoriteStockUseCase favoriteStockUseCase;

    @Operation(summary = "user의 관심 종목 등록", description = "user의 관심 종목에 해당 종목을 등록합니다.")
    @PostMapping
    // TODO: @AuthUser 적용 후 @PathVariable 제거
    public ApiResponseDto<Long> addUserFavoriteStock(
            @PathVariable Long userId,
            @RequestBody FavoriteStockRequestDto favoriteStockRequest) {
        return ApiResponseDto.onSuccess(favoriteStockUseCase.addFavoriteStock(userId, favoriteStockRequest));
    }

    // TODO: @AuthUser 적용 후 @PathVariable 제거
    @Operation(summary = "user의 관심 종목들 목록", description = "user의 관심 종목들을 pagination으로 조회합니다.")
    @GetMapping
    public ApiResponseDto<FavoriteStockPageResponse> getFavoriteStockByPage(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        Pageable pageable = PageRequest.of(page, pageSize,
                Sort.by(Sort.Order.desc("isAlertEnabled"), Sort.Order.asc("ticker")));
        Page<FavoriteStock> favoriteStocksPage = favoriteStockUseCase
                .getFavoriteStockPageByUserId(userId, pageable);

        return ApiResponseDto.onSuccess(FavoriteStockConverter.toFavoriteStockPageDto(favoriteStocksPage));
    }

    @Operation(summary = "관심 종목의 알림 업데이트", description = "user의 favorite-stock에 등록된 종목의 알림을 켜거나 끕니다.")
    @PatchMapping("/{favoriteStockId}")
    // TODO: @AuthUser 적용 후 @PathVariable userId 제거
    public ApiResponseDto<Long> updateFavoriteStock(
            @PathVariable Long userId,
            @PathVariable Long favoriteStockId,
            @RequestParam boolean isEnabled) {
        return ApiResponseDto.onSuccess(
                favoriteStockUseCase.updateFavoriteStockAlarm(userId, favoriteStockId, isEnabled));
    }

    @Operation(summary = "user의 관심 종목에서 종목 삭제", description = "user의 관심 종목에서 해당 종목을 삭제합니다.")
    @DeleteMapping("/{favoriteStockId}")
    // TODO: @AuthUser 적용 후 @PathVariable userId 제거
    public ApiResponseDto<Void> deleteUserFavoriteStock(
            @PathVariable Long userId,
            @PathVariable Long favoriteStockId) {
        favoriteStockUseCase.deleteFavoriteStock(userId, favoriteStockId);
        return ApiResponseDto.onSuccess(null);
    }
}
