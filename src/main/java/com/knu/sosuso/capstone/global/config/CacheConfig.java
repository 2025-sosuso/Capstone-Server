package com.knu.sosuso.capstone.global.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Caffeine 캐시 매니저 설정
     * - 메모리 기반 로컬 캐시
     * - Redis보다 가볍고 빠름
     * - 캐시별로 다른 TTL 적용
     */
    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();

        cacheManager.setCaches(Arrays.asList(
                // 인기 영상 캐시 (1시간 유지)
                buildCache("popularVideos", 60),

                // 영상 상세 캐시 (30분 유지)
                buildCache("videoDetail", 30),

                // 채널 정보 캐시 (1시간 유지)
                buildCache("channelInfo", 60),

                // 관심 채널 캐시 (2시간 유지)
                buildCache("favoriteChannels", 120)
        ));

        log.info("Caffeine 캐시 매니저 초기화 완료 - 캐시별 TTL 적용");
        return cacheManager;
    }

    /**
     * 개별 캐시 생성 메서드
     * @param name 캐시 이름
     * @param ttlMinutes TTL (분 단위)
     * @return CaffeineCache
     */
    private CaffeineCache buildCache(String name, int ttlMinutes) {
        log.info("캐시 생성: name={}, TTL={}분", name, ttlMinutes);

        return new CaffeineCache(name,
                Caffeine.newBuilder()
                        .maximumSize(1000)                              // 최대 1000개 엔트리
                        .expireAfterWrite(ttlMinutes, TimeUnit.MINUTES) // 캐시별 TTL
                        .recordStats()                                   // 캐시 통계 수집
                        .evictionListener((key, value, cause) -> {
                            log.debug("캐시 제거: cache={}, key={}, cause={}",
                                    name, key, cause);
                        })
                        .build());
    }
}