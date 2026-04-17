package com.example.demo.api.favoriteStock.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "[사용자 관심 종목 페이지] 사용자 관심 종목 API")
@RestController
@RequestMapping("/api/v1/users/favorite-stocks")
@RequiredArgsConstructor
public class FavoriteStockController {
}
