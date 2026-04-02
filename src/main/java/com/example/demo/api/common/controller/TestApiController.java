package com.example.demo.api.common.controller;

import com.example.demo.api.common.dto.ApiResponseDto;
import com.example.demo.common.annotation.DisableSwaggerSecurity;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.logging.Logger;

@Slf4j
@Tag(name = "테스트 용 API")
@RestController
@RequestMapping("/api/v1/test")
@RequiredArgsConstructor
public class TestApiController {

    @GetMapping("/health-check")
    public ApiResponseDto<String> healthCheckup() {
        return ApiResponseDto.onSuccess(HttpStatus.OK.toString());
    }

}

