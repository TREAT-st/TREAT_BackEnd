package com.example.demo.domain.favoriteStock.repository;

import com.example.demo.domain.favoriteStock.entity.FavoriteStock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface FavoriteStockRepository extends JpaRepository<FavoriteStock, Long> {
    Page<FavoriteStock> findAllByUserId(Long userId, Pageable pageable);
    Optional<FavoriteStock> findByUserIdAndStockCode(Long userId, String stockCode);
    boolean existsByUserIdAndStockCode(Long userId, String stockCode);

    @Query("SELECT f FROM FavoriteStock f WHERE f.id IN (SELECT MIN(f2.id) FROM FavoriteStock f2 GROUP BY f2.stockCode)")
    List<FavoriteStock> findDistinctStocks();
}
