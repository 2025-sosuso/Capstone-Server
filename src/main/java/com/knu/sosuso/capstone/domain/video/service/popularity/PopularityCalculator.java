package com.knu.sosuso.capstone.domain.video.service.popularity;

import com.knu.sosuso.capstone.domain.scrap.repository.ScrapRepository;
import com.knu.sosuso.capstone.domain.video.entity.VideoStats;
import com.knu.sosuso.capstone.domain.video.repository.VideoStatsRepository;
import com.knu.sosuso.capstone.domain.video.repository.VideoViewLogRepository;
import com.knu.sosuso.capstone.global.config.AppConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 인기도 점수 계산 핵심 로직
 * 배치 작업으로 주기적으로 실행됨
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class PopularityCalculator {

    private final AppConfig appConfig;
    private final VideoViewLogRepository viewLogRepository;
    private final ScrapRepository scrapRepository;
    private final VideoStatsRepository videoStatsRepository;
    private final PopularityScoreStrategy scoreStrategy;

    /**
     * 모든 영상의 인기도 점수 계산 및 업데이트
     *
     * @return 업데이트된 영상 개수
     */
    @Transactional
    public int calculateAndUpdate() {
        log.info("=== 인기도 점수 계산 시작 ===");

        LocalDateTime retentionStartDate = LocalDateTime.now().minusDays(
                appConfig.getPopularityRetentionDays()
        );

        // 1. 데이터 집계
        Map<String, Integer> viewCounts = aggregateViews(retentionStartDate);
        Map<String, Integer> scrapCounts = aggregateScraps();

        log.info("집계 완료: 조회 {}개 영상, 스크랩 {}개 영상",
                viewCounts.size(), scrapCounts.size());

        // 2. 모든 영상 ID 수집
        Set<String> allVideoIds = new HashSet<>();
        allVideoIds.addAll(viewCounts.keySet());
        allVideoIds.addAll(scrapCounts.keySet());

        if (allVideoIds.isEmpty()) {
            log.warn("집계할 데이터가 없습니다");
            return 0;
        }

        // 3. 점수 계산 및 저장
        int updatedCount = 0;
        for (String apiVideoId : allVideoIds) {
            try {
                int viewCount = viewCounts.getOrDefault(apiVideoId, 0);
                int scrapCount = scrapCounts.getOrDefault(apiVideoId, 0);

                // 전략 패턴으로 점수 계산
                double score = scoreStrategy.calculate(viewCount, scrapCount);

                updateVideoStats(apiVideoId, viewCount, scrapCount, score);
                updatedCount++;

            } catch (Exception e) {
                log.error("점수 계산 실패: apiVideoId={}, error={}",
                        apiVideoId, e.getMessage(), e);
            }
        }

        log.info("=== 인기도 점수 계산 완료: {}개 영상 업데이트 ===", updatedCount);
        return updatedCount;
    }

    /**
     * 설정된 기간(N일) 동안의 조회수 집계
     */
    private Map<String, Integer> aggregateViews(LocalDateTime since) {
        List<Object[]> results = viewLogRepository.countViewsByVideoSince(since);

        return results.stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> ((Long) row[1]).intValue()
                ));
    }

    /**
     * 전체 스크랩 횟수 집계
     */
    private Map<String, Integer> aggregateScraps() {
        List<Object[]> results = scrapRepository.countScrapsByVideo();

        return results.stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> ((Long) row[1]).intValue()
                ));
    }

    /**
     * VideoStats 업데이트 또는 생성
     */
    private void updateVideoStats(String apiVideoId, int viewCount,
                                  int scrapCount, double score) {
        VideoStats stats = videoStatsRepository.findById(apiVideoId)
                .orElse(new VideoStats());

        stats.setApiVideoId(apiVideoId);
        stats.setViewCountRecent(viewCount);
        stats.setScrapCount(scrapCount);
        stats.setPopularityScore(score);
        stats.setLastCalculatedAt(LocalDateTime.now());

        videoStatsRepository.save(stats);
    }

    /**
     * 설정된 보관 기간(N일) 이상 지난 조회 로그 삭제
     * 데이터베이스 용량 관리
     */
    @Transactional
    public void cleanupOldLogs() {
        log.info("오래된 조회 로그 삭제 시작");

        LocalDateTime threshold = LocalDateTime.now().minusDays(
                appConfig.getPopularityRetentionDays()
        );
        viewLogRepository.deleteOldLogs(threshold);

        log.info("오래된 조회 로그 삭제 완료");
    }
}