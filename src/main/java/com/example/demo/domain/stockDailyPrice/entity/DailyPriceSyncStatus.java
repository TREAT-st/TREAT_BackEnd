package com.example.demo.domain.stockDailyPrice.entity;

/**
 * 거래일 하나의 일일 적재 결과.
 *
 * SUCCESS·HOLIDAY는 끝난 상태라 다시 받지 않는다. PARTIAL·FAILED는 다음 일일 실행이 다시 시도한다
 * (시도 횟수 상한까지).
 */
public enum DailyPriceSyncStatus {
    /** 요청한 종목을 모두 받았다. 거래정지 등으로 가격이 없는 종목(skipped)은 받은 것으로 친다. */
    SUCCESS,
    /** 일부 종목만 받았다. 실패 종목이 기록되고, 다음 실행은 그 종목만 다시 요청한다. */
    PARTIAL,
    /** 호출 자체가 실패했거나 저장에 실패했다. 요청한 종목 전체가 실패 종목으로 기록된다. */
    FAILED,
    /** 시장 전체 시세가 없는 날(휴장일). 데이터가 없는 것이 정상이다. */
    HOLIDAY;

    public boolean isFinished() {
        return this == SUCCESS || this == HOLIDAY;
    }
}
