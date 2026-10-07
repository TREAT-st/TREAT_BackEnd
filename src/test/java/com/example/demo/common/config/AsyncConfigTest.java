package com.example.demo.common.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 실제 배치 실행기의 동작을 검증한다.
 *
 * E2E 테스트는 검증이 흔들리지 않도록 동기 실행기로 갈아끼우기 때문에,
 * 정작 운영에서 쓰는 이 실행기가 스레드를 나누는지, 겹친 제출을 거부하는지는 여기서만 확인된다.
 */
class AsyncConfigTest {

    private ThreadPoolTaskExecutor executor;

    @AfterEach
    void tearDown() {
        if (executor != null) {
            executor.shutdown();
        }
    }

    private ThreadPoolTaskExecutor batchExecutor() {
        Executor bean = new AsyncConfig().batchTaskExecutor();
        executor = (ThreadPoolTaskExecutor) bean;
        return executor;
    }

    /** 배치가 요청 스레드를 붙들면 202를 즉시 돌려줄 수 없다. */
    @Test
    void 요청_스레드와_다른_스레드에서_실행된다() throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<String> ranOn = new AtomicReference<>();

        batchExecutor().execute(() -> {
            ranOn.set(Thread.currentThread().getName());
            done.countDown();
        });

        assertThat(done.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(ranOn.get())
                .isNotEqualTo(Thread.currentThread().getName())
                .startsWith("batch-");
    }

    /**
     * 큐 용량이 0이라 스레드가 바쁘면 쌓지 않고 즉시 거부한다.
     * 쌓아두면 트리거가 여러 번 들어왔을 때 배치가 그 횟수만큼 연달아 돈다.
     *
     * 컨트롤러는 이 예외를 받아 "이미 실행 중"으로 바꾼다.
     */
    @Test
    void 실행_중이면_두_번째_제출을_거부한다() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        batchExecutor().execute(() -> {
            started.countDown();
            await(release);
        });
        assertThat(started.await(2, TimeUnit.SECONDS)).isTrue();

        assertThatThrownBy(() -> executor.execute(() -> { }))
                .isInstanceOf(TaskRejectedException.class);

        release.countDown();
    }

    /**
     * 앞 작업이 끝나면 다음 트리거는 다시 받아야 한다. 한 번 거부하고 막혀버리면
     * 하루 한 번 도는 배치라 다음 날에야 발견된다.
     *
     * 작업이 끝난 시점과 워커가 큐로 복귀하는 시점 사이에 아주 짧은 틈이 있어,
     * 곧바로 제출하면 간헐적으로 거부된다. "결국 다시 받는다"를 검증하는 게 목적이라
     * 짧게 재시도한다.
     */
    @Test
    void 앞_작업이_끝나면_다시_받는다() throws Exception {
        batchExecutor().submit(() -> { }).get(2, TimeUnit.SECONDS);

        CountDownLatch second = new CountDownLatch(1);
        executeUntilAccepted(second::countDown);

        assertThat(second.await(2, TimeUnit.SECONDS)).isTrue();
    }

    private void executeUntilAccepted(Runnable task) throws InterruptedException {
        for (int attempt = 0; attempt < 50; attempt++) {
            try {
                executor.execute(task);
                return;
            } catch (TaskRejectedException e) {
                TimeUnit.MILLISECONDS.sleep(20);
            }
        }
        throw new AssertionError("실행기가 복귀하지 않아 계속 거부했습니다.");
    }

    private void await(CountDownLatch latch) {
        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
