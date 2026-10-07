package com.example.demo.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 이게 없으면 @Scheduled 메서드가 조용히 실행되지 않는다.
 *
 * 배치 트리거는 EventBridge로 뺐지만 도착 확인 스위퍼는 여기 남는다.
 * 콜백을 받는 게 이 앱이라 앱이 죽으면 스위퍼만 밖에 있어도 소용이 없고,
 * 밖으로 빼면 엔드포인트와 EventBridge 규칙이 하나씩 더 는다.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
