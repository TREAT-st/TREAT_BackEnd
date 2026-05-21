package com.example.demo.infra.S3.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class S3Response {

    private String stockCode; //종목코드
    private String date; //날짜
    private String url;
}