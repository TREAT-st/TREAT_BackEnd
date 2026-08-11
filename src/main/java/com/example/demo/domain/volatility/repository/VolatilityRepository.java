package com.example.demo.domain.volatility.repository;

import com.example.demo.domain.volatility.entity.Volatility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface VolatilityRepository extends JpaRepository<Volatility, Long> {
    List<Volatility> findAllByStockCodeOrderByCreatedDateDesc(String stockCode);
    List<Volatility> findAllByCreatedDateGreaterThanEqualAndCreatedDateLessThanOrderByCreatedDateDesc(LocalDateTime start, LocalDateTime end);
    Optional<Volatility> findFirstByStockCodeAndCreatedDateGreaterThanEqualAndCreatedDateLessThanOrderByCreatedDateDesc(String stockCode, LocalDateTime start, LocalDateTime end);

    @Modifying
    @Query("DELETE FROM Volatility v WHERE v.createdDate >= :start AND v.createdDate < :end")
    void deleteAllByCreatedDateBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
