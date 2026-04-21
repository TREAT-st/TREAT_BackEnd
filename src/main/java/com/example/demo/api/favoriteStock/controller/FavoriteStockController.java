package com.example.demo.api.favoriteStock.controller;

import com.example.demo.api.common.dto.ApiResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "[사용자 관심 종목 페이지] 사용자 관심 종목 API")
@RestController
@RequestMapping("/api/v1/users/favorite-stocks")
@RequiredArgsConstructor
public class FavoriteStockController {
    @Operation(summary = "user의 관심 종목 등록", description = "user의 관심 종목에 해당 종목을 등록합니다.")
    @PostMapping
    public ApiResponseDto<Void> addUserFavoriteStock() {
        return null;
    }
}
