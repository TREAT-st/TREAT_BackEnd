package com.example.demo.domain.stock.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.domain.Sort;

/**
 * 전체 종목 조회의 정렬 기준.
 *
 * 정렬 키를 문자열로 받아 Sort.by에 그대로 넘기면 클라이언트가 임의 필드명을 보낼 수 있고,
 * 존재하지 않는 필드면 500이 난다. 허용 목록을 enum으로 고정한다.
 *
 * 시가총액·관심등록수는 값이 같은 종목이 많아(특히 likeCount는 초기에 전부 0) 순서가
 * 흔들린다. 페이지 경계에서 같은 종목이 중복되거나 누락되지 않도록 stockCode를 2차 키로 둔다.
 */
@Getter
@AllArgsConstructor
public enum StockSortType {

    STOCK_CODE(Sort.by(Sort.Order.asc("stockCode"))),
    MARKET_CAP(Sort.by(Sort.Order.desc("marketCapitalization"), Sort.Order.asc("stockCode"))),
    LIKE_COUNT(Sort.by(Sort.Order.desc("likeCount"), Sort.Order.asc("stockCode")));

    private final Sort sort;
}
