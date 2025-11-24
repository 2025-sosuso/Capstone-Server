package com.knu.sosuso.capstone.global.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 비동기 처리 및 스레드풀 설정
 *
 * 3개의 전용 스레드풀로 작업 격리:
 * 1. urlCheckExecutor - YouTube Shorts URL 병렬 체크 (I/O 중심)
 * 2. searchProcessingExecutor - 검색 결과 백그라운드 처리 (DB + AI)
 * 3. videoProcessingExecutor - 비디오 AI 분석 처리
 */
@Slf4j
@Configuration
@EnableAsync
@EnableScheduling
@EnableRetry
@RequiredArgsConstructor
public class AsyncConfig {

    private final AppConfig appConfig;

    /**
     * URL 체크 전용 스레드풀
     * - 용도: YouTube Shorts URL 병렬 체크
     * - 특징: I/O 작업 중심, 많은 동시 요청 처리
     */
    @Bean(name = "urlCheckExecutor")
    public Executor urlCheckExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(appConfig.getUrlCheckCorePoolSize());
        executor.setMaxPoolSize(appConfig.getUrlCheckMaxPoolSize());
        executor.setQueueCapacity(appConfig.getUrlCheckQueueCapacity());
        executor.setThreadNamePrefix("url-check-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        executor.initialize();
        log.info("✅ URL 체크 전용 스레드풀 초기화: core={}, max={}, queue={}",
                appConfig.getUrlCheckCorePoolSize(),
                appConfig.getUrlCheckMaxPoolSize(),
                appConfig.getUrlCheckQueueCapacity());

        return executor;
    }

    /**
     * 검색 처리 전용 스레드풀
     * - 용도: 검색 결과 백그라운드 처리 (4개 이상 영상)
     * - 특징: DB + AI 혼합 작업, 검색 우선순위 보장
     */
    @Bean(name = "searchProcessingExecutor")
    public Executor searchProcessingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(appConfig.getSearchProcessingCorePoolSize());
        executor.setMaxPoolSize(appConfig.getSearchProcessingMaxPoolSize());
        executor.setQueueCapacity(appConfig.getSearchProcessingQueueCapacity());
        executor.setThreadNamePrefix("search-proc-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        executor.initialize();
        log.info("✅ 검색 처리 전용 스레드풀 초기화: core={}, max={}, queue={}",
                appConfig.getSearchProcessingCorePoolSize(),
                appConfig.getSearchProcessingMaxPoolSize(),
                appConfig.getSearchProcessingQueueCapacity());

        return executor;
    }

    /**
     * 비디오 AI 처리용 스레드풀
     * - 용도: AI 분석, 상세 페이지 처리
     * - 특징: 기존 로직 유지
     */
    @Bean(name = "videoProcessingExecutor")
    public Executor videoProcessingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(appConfig.getVideoProcessingCorePoolSize());
        executor.setMaxPoolSize(appConfig.getVideoProcessingMaxPoolSize());
        executor.setQueueCapacity(appConfig.getVideoProcessingQueueCapacity());
        executor.setThreadNamePrefix("video-proc-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        executor.initialize();
        log.info("✅ 비디오 처리 스레드풀 초기화: core={}, max={}, queue={}",
                appConfig.getVideoProcessingCorePoolSize(),
                appConfig.getVideoProcessingMaxPoolSize(),
                appConfig.getVideoProcessingQueueCapacity());

        return executor;
    }
}