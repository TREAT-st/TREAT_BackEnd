package com.example.demo.common.consts;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class StaticVariable {
    public static final String USER = "type_user";
    public static final String EXPERT = "type_expert";
    public static final String SWAGGER_JWT = "JWT";
    public static final String SWAGGER_BEARER = "Bearer";
    public static final String BEARER = "Bearer ";
    public static final String AUTHORIZATION = "Authorization";
    public static final String REISSUE_ENDPOINT = "/api/v1/tokens/reissue";
    public static final String HEALTH_CHECK_ENDPOINT = "/api/v1/test/health-check";
    public static final String CREATED_DATE = "createdDate";
    public static final String LAST_MODIFIED_DATE = "lastModifiedDate";
    public static final String ADVICE_ID = "id";
    public static final String NOTIFICATION_READ = "isRead";
    public static final String PAGINATION_SORTING_BY_ID  = "id";
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    public static final String REPORT_GENERATION_SUCCESS = "success";
    public static final String REPORT_GENERATION_FAILURE = "failure";
    public static final String REPORT_GENERATION_PARTIAL_SUCCESS = "PARTIAL_SUCCESS";
    public static final String NO_DETECTED_STOCK = "탐지된 종목 없음";
    public static final String ANOTHER_RUN_IN_PROGRESS = "다른 실행이 진행 중입니다";
    public static final String NOT_A_TRADING_DAY = "거래일이 아니라 실행하지 않았습니다.";
    public static final String NOT_A_TRADING_DAY_LOG = "거래일이 아닙니다. KRX 거래일=%s";
    public static final String BATCH_ACCEPTED = "배치를 접수했습니다.";
    public static final String BATCH_NOT_ACCEPTED_ALREADY_RUNNING = "이미 실행 중인 배치가 있어 접수하지 않았습니다.";
    public static final String BATCH_SECRET_HEADER = "X-Batch-Secret";

    //OAuth2
    public static final String KAKAO_OAUTH2_AUTHORIZATION_URI = "/oauth2/authorization/kakao";

    //JWT
    public static final long ACCESS_TOKEN_EXPIRE_TIME = 1000 * 60 * 60 * 24; // 1일
    public static final long REFRESH_TOKEN_EXPIRE_TIME = 1000L * 60 * 60 * 24 * 7; // 7일
    public static final String REFRESH_TOKEN_COOKIE = "refreshToken";

    public static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");
}

