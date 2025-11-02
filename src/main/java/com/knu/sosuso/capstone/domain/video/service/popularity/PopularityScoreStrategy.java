package com.knu.sosuso.capstone.domain.video.service.popularity;

/**
 * 인기도 점수 계산 전략 인터페이스
 * 새로운 알고리즘 추가 시 구현체만 추가하면 됨
 */
public interface PopularityScoreStrategy {

    /**
     * 인기도 점수 계산
     *
     * @param viewCountRecent 최근 N일간 조회수 (AppConfig 설정 기준)
     * @param scrapCount 전체 스크랩 횟수
     * @return 계산된 인기도 점수
     */
    double calculate(int viewCountRecent, int scrapCount);
}