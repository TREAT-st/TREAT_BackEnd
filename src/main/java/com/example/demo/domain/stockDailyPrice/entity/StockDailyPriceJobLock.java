package com.example.demo.domain.stockDailyPrice.entity;

import com.example.demo.domain.model.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * 일봉 적재(백필·일일) 작업의 실행 잠금. 서버가 여러 대여도 한 번에 하나만 돌게 한다.
 *
 * 실행기(dailyPriceTaskExecutor)는 한 서버 안의 중복만 막는다. 서버마다 실행기가 있으므로 서버가 둘이면
 * 둘 다 접수해 같은 Lambda를 중복 호출하고, 같은 날짜의 적재 이력을 동시에 만들다 하나를 잃는다.
 *
 * 잠금은 임대(lease) 방식이다. lockedUntil이 지나면 다른 서버가 가져갈 수 있다. 작업이 끝나면 풀지만,
 * 서버가 작업 중에 죽어도 임대 시간이 지나면 다시 실행할 수 있다.
 * 행은 잠금 이름마다 하나다. 백필과 일일 적재는 서로도 겹치면 안 되므로 같은 이름을 쓴다.
 */
@Entity
@Table(name = "stock_daily_price_job_lock")
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class StockDailyPriceJobLock extends BaseTimeEntity {

    @Id
    @Column(name = "lock_name", length = 50)
    private String lockName;

    /** 잠금을 가진 실행의 토큰. 풀 때 자기 잠금만 풀도록 대조한다. */
    @Column(name = "owner", length = 64)
    private String owner;

    /** 이 시각이 지나면 다른 실행이 가져갈 수 있다. null이면 비어 있다. */
    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    public static StockDailyPriceJobLock empty(String lockName) {
        return StockDailyPriceJobLock.builder().lockName(lockName).build();
    }
}
