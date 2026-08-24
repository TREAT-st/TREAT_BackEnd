package com.example.demo.api.reportRequest.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "[리포트 요청 페이지] 리포트 요청 API")
@RestController
@RequestMapping("/api/v1/report-request")
@RequiredArgsConstructor
public class ReportRequestController {
}
