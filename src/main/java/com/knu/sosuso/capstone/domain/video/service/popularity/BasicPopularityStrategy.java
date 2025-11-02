package com.knu.sosuso.capstone.domain.video.service.popularity;

/**
 * 기본 인기도 점수 계산 전략
 *
 * 점수 = (조회수 × View가중치) + (스크랩 × Scrap가중치)
 *
 * 가중치 설명:
 * - 조회: 관심 표현
 * - 스크랩: 실제 저장 행동, 더 높은 가치
 */
public class BasicPopularityStrategy implements PopularityScoreStrategy {

    private final double viewWeight;
    private final double scrapWeight;

    // 생성자를 통해 가중치를 주입받음
    public BasicPopularityStrategy(double viewWeight, double scrapWeight) {
        this.viewWeight = viewWeight;
        this.scrapWeight = scrapWeight;
    }

    @Override
    public double calculate(int viewCountRecent, int scrapCount) {
        return (viewCountRecent * viewWeight) + (scrapCount * scrapWeight);
    }
}