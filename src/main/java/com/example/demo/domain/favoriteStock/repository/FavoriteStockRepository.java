package com.example.demo.domain.favoriteStock.repository;

import com.example.demo.domain.favoriteStock.entity.FavoriteStock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.Set;

public interface FavoriteStockRepository extends JpaRepository<FavoriteStock, Long> {
    Page<FavoriteStock> findAllByUserId(Long userId, Pageable pageable);
    Optional<FavoriteStock> findByUserIdAndStockCode(Long userId, String stockCode);
    boolean existsByUserIdAndStockCode(Long userId, String stockCode);

    @Query("select f.stockCode from FavoriteStock f where f.user.id = :userId")
    Set<String> findStockCodesByUserId(@Param("userId") Long userId);
}
