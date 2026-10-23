package com.example.demo.common.consts;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;

public class StaticVariable {
    public static final String USER = "type_user";
    public static final String SWAGGER_JWT = "JWT";
    public static final String SWAGGER_BEARER = "Bearer";
    public static final String BEARER = "Bearer ";
    /** 토큰 응답의 grantType. 헤더 접두사(BEARER)와 달리 공백이 없다. */
    public static final String GRANT_TYPE_BEARER = "Bearer";
    public static final String AUTHORIZATION = "Authorization";
    public static final String REISSUE_ENDPOINT = "/api/v1/tokens/reissue";
    public static final String HEALTH_CHECK_ENDPOINT = "/api/v1/test/health-check";
    public static final String CREATED_DATE = "createdDate";
    /**
     * yyyyMMdd. KRX 거래일, 리포트 날짜 등 서버 전체가 쓰는 날짜 표기다.
     * STRICT 해석이라 20260231 같은 존재하지 않는 날짜를 보정하지 않고 거부한다.
     * STRICT에서는 연도 필드로 yyyy(연호 기준) 대신 uuuu(proleptic)를 써야 한다.
     */
    public static final DateTimeFormatter YYYYMMDD_FORMATTER =
            DateTimeFormatter.ofPattern("uuuuMMdd").withResolverStyle(ResolverStyle.STRICT);
    public static final String REPORT_GENERATION_SUCCESS = "SUCCESS";
    public static final String REPORT_GENERATION_FAILURE = "FAILURE";
    public static final String REPORT_GENERATION_PARTIAL_SUCCESS = "PARTIAL_SUCCESS";
    public static final String NO_DETECTED_STOCK = "탐지된 종목 없음";
    public static final String ANOTHER_RUN_IN_PROGRESS = "다른 실행이 진행 중입니다";
    public static final String SYNC_ALREADY_APPLIED = "이미 반영된 거래일이라 DB 동기화를 생략했습니다.";
    public static final String BATCH_ACCEPTED = "배치를 접수했습니다.";
    public static final String BATCH_NOT_ACCEPTED_ALREADY_RUNNING = "이미 실행 중인 배치가 있어 접수하지 않았습니다.";
    public static final String DAILY_PRICE_BACKFILL_ACCEPTED = "일봉 백필을 접수했습니다.";
    public static final String DAILY_PRICE_DAILY_ACCEPTED = "일봉 일일 적재를 접수했습니다.";
    public static final String DAILY_PRICE_NOTHING_TO_FILL = "받을 일봉이 없습니다. 이미 최신입니다.";
    public static final String DAILY_PRICE_BACKFILL_NO_TARGET = "백필할 종목이 없습니다.";
    public static final String DAILY_PRICE_ALREADY_RUNNING = "다른 일봉 적재(백필·일일)가 실행 중이라 접수하지 않았습니다.";
    public static final String DAILY_PRICE_SYNC_HOLIDAY = "휴장일";
    public static final String DAILY_PRICE_SYNC_FAILED_COUNT = "실패 %d종목";
    /**
     * 일일 적재가 이력이 없는 평일을 다시 찾는 범위(달력 기준, 최근 거래일에서 거꾸로).
     * 이 안의 빈 날은 다음 일일 적재가 받는다. 이보다 오래된 구간은 다시 찾지 않는다(정리 목적 삭제를 되살리지 않도록).
     */
    public static final int DAILY_PRICE_SYNC_LOOKBACK_DAYS = 30;
    /** 일일 적재 자동 재시도 상한. 이 횟수를 채운 PARTIAL·FAILED 날짜는 자동으로 다시 받지 않는다. */
    public static final int DAILY_PRICE_SYNC_MAX_ATTEMPT = 5;
    public static final String DAILY_PRICE_REFILL_HINT = "전 종목을 최근 " + DAILY_PRICE_SYNC_LOOKBACK_DAYS
            + "일 안에서 지웠으면 다음 일일 적재가 다시 받습니다. "
            + "그보다 오래된 구간이나 한 종목만 지웠으면 백필을 같은 기간(한 종목이면 stockCodes)으로 호출하세요.";
    public static final String VERIFY_ALL_REPORTS_ARRIVED = "리포트가 모두 도착했습니다.";
    public static final String VERIFY_MISSING_REPORT = "리포트 %d건이 도착하지 않았습니다.";
    public static final String VERIFY_RECOVERED_FROM_S3 = "S3에서 %d건을 보정해 리포트가 모두 채워졌습니다.";
    public static final String VERIFY_S3_LOOKUP_FAILED = " S3 조회 실패 %d건.";
    public static final String BATCH_SECRET_HEADER = "X-Batch-Secret";
    /** 리포트 콜백(S3 이벤트 Lambda) 인증 헤더. 배치 시크릿과 다른 값을 쓴다. */
    public static final String CALLBACK_SECRET_HEADER = "X-Callback-Secret";

    //OAuth2
    public static final String KAKAO_OAUTH2_AUTHORIZATION_URI = "/oauth2/authorization/kakao";

    //JWT
    public static final long ACCESS_TOKEN_EXPIRE_TIME = 1000 * 60 * 60 * 24; // 1일
    public static final long REFRESH_TOKEN_EXPIRE_TIME = 1000L * 60 * 60 * 24 * 7; // 7일
    public static final String REFRESH_TOKEN_COOKIE = "refreshToken";

    /** 문자열이 필요한 곳(@Scheduled의 zone 등 어노테이션 속성)에 쓴다. */
    public static final String SEOUL_ZONE_ID = "Asia/Seoul";
    public static final ZoneId SEOUL_ZONE = ZoneId.of(SEOUL_ZONE_ID);

    //Chart
    public static final String CHART_RANGE_REGEXP = "1W|1M|3M|1Y|3Y";
    public static final String CHART_RANGE_MESSAGE = "range는 1W, 1M, 3M, 1Y, 3Y 중 하나입니다.";
}

