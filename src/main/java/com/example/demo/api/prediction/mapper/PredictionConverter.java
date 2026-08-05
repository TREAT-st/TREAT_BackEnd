package com.example.demo.api.prediction.mapper;

import com.example.demo.api.prediction.dto.PredictionResponseDto.*;
import com.example.demo.domain.prediction.entity.Prediction;
import com.example.demo.domain.prediction.entity.PredictionDuration;
import com.example.demo.domain.prediction.entity.PredictionStatus;
import com.example.demo.domain.prediction.entity.PredictionTarget;
import com.example.demo.domain.stock.entity.Stock;
import com.example.demo.domain.user.entity.User;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public class PredictionConverter {

    private static final int BASE_POINT = 1;
    private static final LocalTime MARKET_CLOSE = LocalTime.of(15, 30);

    /**
     * 제출일 기준 N 캘린더 일 뒤 15:30을 만기로 설정.
     * 만기일이 토요일이면 -1일(금), 일요일이면 -2일(금)로 당김.
     */
    private static LocalDateTime calcMaturityAt(int days) {
        LocalDate date = LocalDate.now().plusDays(days);
        if (date.getDayOfWeek() == DayOfWeek.SATURDAY) {
            date = date.minusDays(1);
        } else if (date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            date = date.minusDays(2);
        }
        return date.atTime(MARKET_CLOSE);
    }

    /** 포인트 = 기본점수 × duration 가중치 × target 가중치 (문구: 목표치↑, duration↓ 일수록 高) */
    public static int calcPossiblePoint(PredictionDuration duration, PredictionTarget target) {
        return BASE_POINT * duration.getWeight() * target.getWeight();
    }

    /** 제출 시 Prediction 엔티티 생성 */
    public static Prediction toPrediction(User user, Stock stock, PredictionDuration duration,
                                          PredictionTarget target, BigDecimal basePrice) {
        int possiblePoint = calcPossiblePoint(duration, target);
        return Prediction.builder()
                .user(user)
                .stock(stock)
                .duration(duration)
                .target(target)
                .status(PredictionStatus.PENDING)
                .possiblePoint(possiblePoint)
                .basePrice(basePrice)
                .maturityAt(calcMaturityAt(duration.getDays()))
                .build();
    }

    public static CreatePredictionResult toCreateResult(Prediction p) {
        return CreatePredictionResult.builder()
                .predictionId(p.getId())
                .possiblePoint(p.getPossiblePoint())
                .status(p.getStatus())
                .build();
    }

    public static PredictionResult toResult(Prediction p) {
        boolean pending = p.isPending();
        boolean correct = p.getStatus() == PredictionStatus.CORRECT;
        boolean rose = !pending && p.getActualRate() != null
                && p.getActualRate().signum() > 0;

        String title;
        String subtitle;
        if (pending) {
            title = "채점 대기중";
            subtitle = "만기가 도래하면 결과를 확인할 수 있어요!";
        } else if (correct) {
            title = "예측 적중!";
            subtitle = "시장의 흐름을 완벽하게 읽었어요!";
        } else {
            title = "예측이 빗나갔어요...";
            subtitle = "시장의 흐름을 다시 한번 살펴보세요!";
        }

        return PredictionResult.builder()
                .predictionId(p.getId())
                .status(p.getStatus())
                .myPredictionText(p.getTarget().getDescription())
                .actualRate(p.getActualRate())
                .earnedPoint(p.getEarnedPoint())
                .title(title)
                .subtitle(subtitle)
                .reasonHeader(pending ? null : (rose ? "왜 올랐을까?" : "왜 떨어졌을까?"))
                .reasons(pending ? List.of() : dummyReasons())
                .build();
    }

    // 2-C: 더미 원인 데이터
    private static List<Reason> dummyReasons() {
        return List.of(
                Reason.builder()
                        .title("AI 메모리 분야 역대 최고 매출 기록")
                        .content("HBM3 등 고부가 가치 제품의 수요급증으로 반도체 부문 영업이익이 크게 개선되었습니다.")
                        .build(),
                Reason.builder()
                        .title("파운드리 수주 확대 및 수율 안정화")
                        .content("글로벌 빅테크 기업들의 선단 공정 수주가 늘어나며 미래 성장 동력을 확보했습니다.")
                        .build()
        );
    }
}
