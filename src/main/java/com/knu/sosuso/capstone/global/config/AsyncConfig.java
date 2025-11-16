package com.knu.sosuso.capstone.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
@EnableScheduling
@EnableRetry
@RequiredArgsConstructor
public class AsyncConfig {

    private final AppConfig appConfig;

    /**
     * 비디오 AI 처리용 Thread Pool
     */
    @Bean(name = "videoProcessingExecutor")
    public Executor videoProcessingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(appConfig.getVideoProcessingCorePoolSize());
        executor.setMaxPoolSize(appConfig.getVideoProcessingMaxPoolSize());
        executor.setQueueCapacity(appConfig.getVideoProcessingQueueCapacity());
        executor.setThreadNamePrefix("ai-processing-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        executor.initialize();
        return executor;
    }
}