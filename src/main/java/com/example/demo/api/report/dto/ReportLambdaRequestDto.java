package com.example.demo.api.report.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReportLambdaRequestDto {
    private String jobId;
    private String stockCode;
    private String stockName;
    private String reportDate;
    private String triggerType;
    private String gptModel;
}
