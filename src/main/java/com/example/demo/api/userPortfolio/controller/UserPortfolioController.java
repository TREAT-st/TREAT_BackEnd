package com.example.demo.api.userPortfolio.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "[사용자 포트폴리오 페이지] 사용자 포트폴리오 API")
@RestController
@RequestMapping("/api/v1/users/portfolio")
@RequiredArgsConstructor
public class UserPortfolioController {
}
