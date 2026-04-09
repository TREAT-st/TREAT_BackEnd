package com.example.demo.domain.reportRequest.entity;

import com.example.demo.domain.favoriteStock.entity.FavoriteStock;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReportRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_request_id")
    private Long id;


}
