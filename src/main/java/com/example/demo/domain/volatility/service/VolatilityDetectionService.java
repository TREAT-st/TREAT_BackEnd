package com.example.demo.domain.volatility.service;

import java.util.List;

public interface VolatilityDetectionService {

    /** KOSPI 시총 상위 topN 종목을 분석해 종목별 변동성 신호를 반환한다. */
    List<VolatilitySignal> detect(int topN);

    /** 분석 결과 중 alert=true인 종목만, 변동성 점수(0.7)와 시총 가중치(0.3)를 결합해 상위 topN개를 선정한다. */
    List<VolatilitySignal> selectTop(List<VolatilitySignal> signals, int universeSize, int topN);
}
