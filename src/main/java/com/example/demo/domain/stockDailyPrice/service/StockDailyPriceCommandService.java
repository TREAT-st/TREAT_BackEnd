package com.example.demo.domain.stockDailyPrice.service;

import com.example.demo.domain.stockDailyPrice.entity.DailyPriceRow;
import com.example.demo.domain.stockDailyPrice.entity.DailyPriceSyncResult;
import com.example.demo.domain.stockDailyPrice.entity.StockDailyPriceDeleteResult;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface StockDailyPriceCommandService {

    /**
     * 일봉을 저장한다. 같은 (종목코드, 거래일)이 이미 있으면 값을 갱신한다.
     * 백필과 일일 적재가 함께 쓴다. DailyPriceRow.isValid()를 통과하지 못한 행은 버린다(최종 방어).
     *
     * @return 실제로 저장(신규+갱신)한 행 수
     */
    int saveDailyPrices(List<DailyPriceRow> rows);

    /** 거래일 하나의 일일 적재 결과를 기록한다. 이미 있으면 덮어쓰고 시도 횟수를 올린다. */
    void recordSyncResult(LocalDate tradeDate, DailyPriceSyncResult result);

    /**
     * 기간의 일봉을 지운다. 양 끝 포함.
     *
     * stockCode가 없으면 전 종목을 지우고, 그 기간의 일일 적재 이력도 함께 지운다.
     * 이력이 SUCCESS인데 데이터가 없는 상태를 남기지 않기 위해서다. 최근 30일 안이면 다음 일일 적재가 다시 받는다.
     * stockCode가 있으면 그 종목만 지우고 이력은 그대로 둔다. 이력은 날짜 단위라 한 종목 삭제를 표현할 수 없다.
     */
    StockDailyPriceDeleteResult deleteDailyPrices(LocalDate from, LocalDate to, String stockCode);

    /** 잠금 행이 없으면 만든다. 서버 둘이 동시에 만들면 한쪽은 유니크 위반이 나는데, 호출자가 무시하면 된다. */
    void createJobLockIfAbsent(String lockName);

    /**
     * 잠금을 가져간다. 비어 있거나 임대가 끝났을 때만 성공한다.
     *
     * @return 가져갔으면 true
     */
    boolean tryAcquireJobLock(String lockName, String owner, LocalDateTime now, LocalDateTime until);

    /**
     * 자기 잠금의 임대를 연장한다. 긴 작업이 단계마다 부른다.
     *
     * @return 연장했으면 true. false면 임대가 끝나 다른 실행이 잠금을 가져간 것이다
     */
    boolean extendJobLock(String lockName, String owner, LocalDateTime until);

    /** 자기 잠금을 푼다. */
    void releaseJobLock(String lockName, String owner);
}
