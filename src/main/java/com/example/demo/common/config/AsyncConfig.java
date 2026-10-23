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

    /**
     * 차트 일봉 채우기 전용 실행기. 일일 배치와 같은 이유로 스레드 하나에 큐를 두지 않는다.
     *
     * 일일 배치 실행기와 나눈 이유는, 첫 3년치 백필처럼 수 분 걸리는 작업이 그 실행기를 차지하면
     * 그동안 들어온 일일 배치 트리거가 거부되어 그날 배치가 빠지기 때문이다.
     * 둘은 쓰는 테이블과 부르는 Lambda가 달라 같은 서버에서 동시에 돌아도 된다.
     * 이 실행기는 한 서버 안의 중복만 막는다. 서버 간 중복은 DB 잠금(DailyPriceJobGuard)이 막는다.
     * 백필은 Lambda 호출 중에 DB 커넥션을 잡지 않고, 묶음 저장만 짧은 트랜잭션으로 한다.
     */
    @Bean("dailyPriceTaskExecutor")
    public Executor dailyPriceTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(0);
        executor.setThreadNamePrefix("daily-price-");
        executor.initialize();
        return executor;
    }
}
