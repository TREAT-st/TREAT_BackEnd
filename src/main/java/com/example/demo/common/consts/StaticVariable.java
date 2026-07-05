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
    public static final String GRANT_TYPE = "authorization_code";
    public static final String REISSUE_ENDPOINT = "/api/v1/tokens/reissue";
    public static final String HEALTH_CHECK_ENDPOINT = "/api/v1/test/health-check";
    public static final String CREATED_DATE = "createdDate";
    public static final String LAST_MODIFIED_DATE = "lastModifiedDate";
    public static final String ADVICE_ID = "id";
    public static final String NOTIFICATION_READ = "isRead";
    public static final String PAGINATION_SORTING_BY_ID  = "id";
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    public static final long MAX_FILE_SIZE = 10 * 1024 * 1024L;
    public static final String XLSX_EXTENSION = ".xlsx";
    public static final String XLSX_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    public static final String KOSPI200_FILE_KEY = "kospi200.xlsx";

    //OAuth2
    public static final String KAKAO_OAUTH2_AUTHORIZATION_URI = "/oauth2/authorization/kakao";

    //JWT
    public static final long ACCESS_TOKEN_EXPIRE_TIME = 1000 * 60 * 60 * 24; // 1일
    public static final long REFRESH_TOKEN_EXPIRE_TIME = 1000L * 60 * 60 * 24 * 7; // 7일
    public static final String REFRESH_TOKEN_COOKIE = "refreshToken";

    //KIS
    public static final String TOKEN_KEY = "kis:access_token";
    public static final DateTimeFormatter EXPIRED_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");
}

