package com.test.crawler.task;

import com.test.crawler.utils.ThreadPoolUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class Executors {
    private static final int CPU_NUM = Runtime.getRuntime().availableProcessors();

    public static final ThreadPoolExecutor ASYNC_SERVICE_EXECUTOR = init("async-service");

    private static ThreadPoolExecutor init(String poolName) {
        return new ThreadPoolExecutor(CPU_NUM, CPU_NUM, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(5000), new ThreadFactory() {
                    private AtomicInteger count = new AtomicInteger(0);

                    @Override
                    public Thread newThread(Runnable r) {
                        int index = count.incrementAndGet();
                        Thread thread = new Thread(r, poolName + "-" + String.valueOf(index));
                        thread.setUncaughtExceptionHandler((t, e) -> {
                            log.error("uncaught exception.", e);
                        });
                        return thread;
                    }
                }, (r, executor) -> {
                    log.error("executor pool exhaust. {}, {}, {}, {}", poolName, executor.getActiveCount(), executor.getTaskCount(), executor.getQueue().size());
                });
    }

    public static final ScheduledExecutorService EXECUTOR_CHECK_TASK = java.util.concurrent.Executors.newScheduledThreadPool(CPU_NUM,
            new ThreadFactory() {
                private AtomicInteger count = new AtomicInteger(0);
                @Override
                public Thread newThread(Runnable r) {
                    int index = count.incrementAndGet();
                    Thread thread = new Thread(r, "executor-check-task" + String.valueOf(index));
                    thread.setUncaughtExceptionHandler((t, e) -> {
                        log.error("uncaught exception.", e);
                    });
                    return thread;
                }
            });

    static {
        EXECUTOR_CHECK_TASK.scheduleWithFixedDelay((Runnable) () -> {
            log.info("async service counter ASYNC_SERVICE_EXECUTOR {}, {}, {}",
                    ASYNC_SERVICE_EXECUTOR.getActiveCount(), ASYNC_SERVICE_EXECUTOR.getTaskCount(), ASYNC_SERVICE_EXECUTOR.getQueue().size());
        }, 10, 10, TimeUnit.SECONDS);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            ThreadPoolUtils.stop(ASYNC_SERVICE_EXECUTOR);
            ThreadPoolUtils.stop(EXECUTOR_CHECK_TASK);
        }));
    }
}
