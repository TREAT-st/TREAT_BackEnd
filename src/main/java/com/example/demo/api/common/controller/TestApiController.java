package com.example.demo.api.common.controller;

import com.example.demo.api.common.dto.ApiResponseDto;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Tag(name = "[더미데이터/동작확인]")
@RestController
@RequestMapping("/api/v1/test")
@RequiredArgsConstructor
public class TestApiController {

    @GetMapping("/health-check")
    public ApiResponseDto<String> healthCheckup() {
        return ApiResponseDto.onSuccess(HttpStatus.OK.toString());
    }

}

