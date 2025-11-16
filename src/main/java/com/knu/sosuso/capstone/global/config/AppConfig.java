package com.knu.sosuso.capstone.global.config;

import com.knu.sosuso.capstone.domain.video.service.popularity.BasicPopularityStrategy;
import com.knu.sosuso.capstone.domain.video.service.popularity.PopularityScoreStrategy;
import lombok.Getter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 애플리케이션 전역 설정
 * 모든 기간/개수 관련 설정을 한 곳에서 관리
 */
@Getter
@Configuration
public class AppConfig {

    // ========== 비디오 관련 설정 ==========

    /**
     * 메타데이터 갱신 주기 (일)
     * 조회수, 좋아요, 댓글 수 등을 얼마나 자주 업데이트할지
     */
    private final int metadataUpdateDays = 30;

    /**
     * YouTube 삭제 여부 확인 주기 (일)
     * 영상이 삭제되었는지 얼마나 자주 확인할지
     */
    private final int deletionCheckDays = 7;

    /**
     * 데이터 보관 기간 (일)
     * 삭제된 영상을 이 기간 동안 보관 후 하드 삭제
     */
    private final int dataRetentionDays = 30;

    /**
     * 감정 흐름 분석 최대 데이터 포인트 수
     * 전체 기간을 이 개수로 샘플링
     */
    private final int sentimentFlowMaxDataPoints = 6;

    // ========== 인기도 계산 설정 ==========

    /**
     * 조회 로그 보관 기간 (일)
     */
    private final int popularityRetentionDays = 7;

    /**
     * 기본 전략: 조회수 가중치
     */
    private final double popularityViewWeight = 1.0;

    /**
     * 기본 전략: 스크랩 가중치
     */
    private final double popularityScrapWeight = 3.0;

    // ========== 댓글 관련 설정 ==========

    /**
     * 댓글 최대 수집 개수
     * YouTube API 비용 고려하여 제한
     */
    private final int maxCommentFetchCount = 100;

    /**
     * API 요청당 최대 결과 수
     */
    private final int maxResultsPerRequest = 100;

    /**
     * 댓글 TOP N 개수
     */
    private final int topCommentsCount = 5;

    // ========== 검색 관련 설정 ==========

    /**
     * 검색 결과 배치 크기 (YouTube API 한 번에 가져올 개수)
     * Search API + Videos API 배치 호출에 사용
     */
    private final int searchBatchSize = 20;

    /**
     * 프론트엔드 페이지 크기 (한 번에 보낼 결과 개수)
     * 사용자가 스크롤할 때마다 4개씩 추가 표시
     */
    private final int searchPageSize = 4;

    /**
     * 검색 결과 최대 페이지 수
     * 무한 스크롤 제한 (총 searchResultsPerPage * maxSearchPages 개)
     */
    private final int maxSearchPages = 5;

    /**
     * 검색 결과 최대 재시도 횟수
     */
    private final int maxRetryPages = 3;

    // ========== 메인 페이지 설정 ==========

    /**
     * 메인 페이지 인기 영상 섹션 표시 개수
     */
    private final int mainPageTrendingCount = 3;

    /**
     * 메인 페이지 스크랩 섹션 표시 개수
     */
    private final int mainPageScrapCount = 3;

    // ========== 캐시 설정 ==========

    /**
     * 영상 상세 캐시 TTL (분)
     */
    private final int cacheVideoDetailTtlMinutes = 30;

    /**
     * 인기 영상 캐시 TTL (분)
     */
    private final int cachePopularVideosTtlMinutes = 60;

    /**
     * 채널 정보 캐시 TTL (분)
     */
    private final int cacheChannelInfoTtlMinutes = 60;

    // ========== AI 배치 처리 설정 ==========

    /**
     * AI 배치 처리 주기 (밀리초)
     * 60000 = 1분
     */
    private final long aiBatchIntervalMs = 60000L;

    /**
     * 배치당 처리할 영상 수
     */
    private final int aiBatchSize = 2;

    // ========== 스레드풀 관련 설정 ==========

    /**
     * 비디오 처리 스레드풀 - 기본 스레드 수
     */
    private final int videoProcessingCorePoolSize = 2;

    /**
     * 비디오 처리 스레드풀 - 최대 스레드 수
     */
    private final int videoProcessingMaxPoolSize = 5;

    /**
     * 비디오 처리 스레드풀 - 큐 크기
     */
    private final int videoProcessingQueueCapacity = 50;


    /**
     * 인기도 계산 전략 Bean 등록
     * @return "basicStrategy"라는 이름의 Bean
     */
    @Bean("basicStrategy")
    public PopularityScoreStrategy basicPopularityStrategy() {
        return new BasicPopularityStrategy(
                popularityViewWeight,
                popularityScrapWeight
        );
    }
}