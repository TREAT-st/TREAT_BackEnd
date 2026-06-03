package com.example.demo.api.kis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class KisTokenRequestDto {

    @JsonProperty("grant_type")
    private String grantType;

    @JsonProperty("appkey")
    private String appKey;

    @JsonProperty("appsecret")
    private String appSecret;
}