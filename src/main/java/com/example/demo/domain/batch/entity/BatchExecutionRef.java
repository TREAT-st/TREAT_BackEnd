package com.example.demo.domain.batch.entity;

/**
 * 실행 중인 한 "시도"를 가리킨다.
 *
 * 재시작은 같은 행을 재사용하므로 executionId만으로는 시도를 구분할 수 없다.
 * 타임아웃으로 인계된 뒤 이전 실행이 뒤늦게 끝나 결과를 쓰면, 지금 돌고 있는 실행의 상태를
 * 남이 SUCCESS/FAILED로 바꿔버린다. attempt를 함께 들고 다녀야 그걸 막을 수 있다.
 */
public record BatchExecutionRef(Long executionId, int attempt) {
}
