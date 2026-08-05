package com.example.demo.domain.prediction.repository;

import com.example.demo.domain.prediction.entity.Prediction;
import com.example.demo.domain.prediction.entity.PredictionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface PredictionRepository extends JpaRepository<Prediction, Long> {

    // 스케줄러: 만기 지난 PENDING 예측 채점 대상 조회
    List<Prediction> findByStatusAndMaturityAtBefore(PredictionStatus status, LocalDateTime now);

    // 마이페이지: 사용자 예측 게임 플레이 횟수
    long countByUserId(Long userId);
}
