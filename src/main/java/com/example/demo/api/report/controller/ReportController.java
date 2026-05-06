package com.example.demo.api.report.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "[사용자 리포트 페이지] 사용자 리포트 API")
@RestController
@RequestMapping("/api/v1/users/reports")
@RequiredArgsConstructor
public class ReportController {
}
