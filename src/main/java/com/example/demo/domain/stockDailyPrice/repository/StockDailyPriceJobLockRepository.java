package com.example.demo.domain.stockDailyPrice.repository;

import com.example.demo.domain.stockDailyPrice.entity.StockDailyPriceJobLock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface StockDailyPriceJobLockRepository extends JpaRepository<StockDailyPriceJobLock, String> {

    /**
     * 비어 있거나 임대가 끝난 잠금만 가져간다. 조건부 UPDATE 한 문장이라 서버 둘이 동시에 시도해도 하나만 1을 받는다.
     *
     * @return 가져갔으면 1, 아니면 0
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update StockDailyPriceJobLock l set l.owner = :owner, l.lockedUntil = :until "
            + "where l.lockName = :lockName and (l.lockedUntil is null or l.lockedUntil < :now)")
    int tryLock(@Param("lockName") String lockName, @Param("owner") String owner,
                @Param("now") LocalDateTime now, @Param("until") LocalDateTime until);

    /**
     * 자기 잠금의 임대를 연장한다. 임대가 끝나 다른 실행이 가져갔으면 owner가 달라 0을 돌려준다.
     *
     * @return 연장했으면 1, 잠금을 잃었으면 0
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update StockDailyPriceJobLock l set l.lockedUntil = :until "
            + "where l.lockName = :lockName and l.owner = :owner")
    int extend(@Param("lockName") String lockName, @Param("owner") String owner, @Param("until") LocalDateTime until);

    /** 자기 잠금만 푼다. 임대가 끝나 다른 실행이 가져간 뒤라면 아무것도 하지 않는다. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update StockDailyPriceJobLock l set l.owner = null, l.lockedUntil = null "
            + "where l.lockName = :lockName and l.owner = :owner")
    int unlock(@Param("lockName") String lockName, @Param("owner") String owner);
}
