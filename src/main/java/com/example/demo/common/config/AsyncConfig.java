package com.example.demo.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@EnableAsync
@Configuration
public class AsyncConfig {

    /**
     * 일일 배치 전용 실행기. 스레드 하나에 큐를 두지 않는다.
     *
     * 큐가 있으면 트리거가 여러 번 들어왔을 때 순서대로 전부 실행된다. 배치는 하루 한 번이면
     * 충분하므로 쌓아둘 이유가 없다. 큐 용량이 0이면 SynchronousQueue를 쓰게 되어,
     * 스레드가 바쁠 때 들어온 요청은 TaskRejectedException으로 즉시 거부된다.
     * 컨트롤러가 그걸 받아 "이미 실행 중"으로 돌려준다.
     *
     * 실행 이력의 IN_PROGRESS 판정과 이중 방어다.
     * 이쪽은 같은 서버 안의 중복을, 그쪽은 서버가 여러 대여도 막는다.
     */
    @Bean("batchTaskExecutor")
    public Executor batchTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(0);
        executor.setThreadNamePrefix("batch-");
        executor.initialize();
        return executor;
    }
}
