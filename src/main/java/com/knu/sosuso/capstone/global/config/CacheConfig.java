package com.knu.sosuso.capstone.global.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Caffeine 캐시 매니저 설정
     * - 메모리 기반 로컬 캐시
     * - Redis보다 가볍고 빠름
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                "popularVideos",    // 인기 영상 (1시간 유지)
                "videoDetail",      // 영상 상세 (30분 유지)
                "channelInfo"       // 채널 정보 (1시간 유지)
        );

        cacheManager.setCaffeine(defaultCaffeineConfig());

        log.info("Caffeine 캐시 매니저 초기화 완료");
        return cacheManager;
    }

    /**
     * 기본 Caffeine 설정
     */
    private Caffeine<Object, Object> defaultCaffeineConfig() {
        return Caffeine.newBuilder()
                .maximumSize(1000)                          // 최대 1000개 엔트리
                .expireAfterWrite(30, TimeUnit.MINUTES)     // 기본 30분 TTL
                .recordStats()                               // 캐시 통계 수집
                .evictionListener((key, value, cause) -> {
                    log.debug("캐시 제거: key={}, cause={}", key, cause);
                });
    }
}