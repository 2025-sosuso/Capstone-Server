package com.knu.sosuso.capstone.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * 비디오 AI 처리용 Thread Pool
     * - Core: 2개 스레드 (기본 대기)
     * - Max: 5개 스레드 (최대 동시 처리)
     * - Queue: 50개 (대기열)
     */
    @Bean(name = "videoProcessingExecutor")
    public Executor videoProcessingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 기본 스레드 수
        executor.setCorePoolSize(2);

        // 최대 스레드 수
        executor.setMaxPoolSize(5);

        // 큐 크기
        executor.setQueueCapacity(50);

        // 스레드 이름 prefix
        executor.setThreadNamePrefix("ai-processing-");

        // 큐가 꽉 찼을 때 정책: 호출한 스레드에서 직접 실행
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        executor.initialize();
        return executor;
    }
}