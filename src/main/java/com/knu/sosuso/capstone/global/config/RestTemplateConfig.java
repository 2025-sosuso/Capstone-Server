package com.knu.sosuso.capstone.global.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate 설정
 *
 * 타임아웃 미설정 시 외부 서버 장애가 우리 서비스로 전파됨
 * - YouTube 서버 지연 → 스레드 무한 대기 → 서비스 마비
 */
@Slf4j
@Configuration
public class RestTemplateConfig {

    /**
     * 기본 RestTemplate (YouTube API 등)
     * - 연결: 5초
     * - 읽기: 10초
     */
    @Bean
    @Primary
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);    // 5초
        factory.setReadTimeout(10000);      // 10초

        log.info("✅ RestTemplate 초기화: connectTimeout=5s, readTimeout=10s");
        return new RestTemplate(factory);
    }

    /**
     * AI 분석용 RestTemplate (FastAPI)
     * - 연결: 5초
     * - 읽기: 60초 (1분)
     */
    @Bean("aiRestTemplate")
    public RestTemplate aiRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);     // 5초
        factory.setReadTimeout(60000);      // 60초 (1분)

        log.info("✅ AI RestTemplate 초기화: connectTimeout=5s, readTimeout=60s");
        return new RestTemplate(factory);
    }

    /**
     * 빠른 응답용 RestTemplate (Shorts URL 체크)
     * - 연결: 2초
     * - 읽기: 3초
     */
    @Bean("fastRestTemplate")
    public RestTemplate fastRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);    // 2초
        factory.setReadTimeout(3000);       // 3초

        log.info("✅ Fast RestTemplate 초기화: connectTimeout=2s, readTimeout=3s");
        return new RestTemplate(factory);
    }
}