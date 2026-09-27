package com.example.demo.domain.prediction.repository;

import com.example.demo.domain.prediction.entity.Prediction;
import com.example.demo.domain.prediction.entity.PredictionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface PredictionRepository extends JpaRepository<Prediction, Long> {
    List<Prediction> findAllByStatusAndMaturityAtBefore(PredictionStatus status, LocalDateTime now);
}