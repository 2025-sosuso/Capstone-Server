package com.knu.sosuso.capstone.domain.video.service;

import com.knu.sosuso.capstone.domain.video.dto.response.VideoSummaryResponse;
import com.knu.sosuso.capstone.domain.video.entity.Video;
import com.knu.sosuso.capstone.domain.video.entity.VideoStats;
import com.knu.sosuso.capstone.domain.video.repository.VideoRepository;
import com.knu.sosuso.capstone.domain.video.repository.VideoStatsRepository;
import com.knu.sosuso.capstone.domain.video.service.popularity.PopularityCalculator;
import com.knu.sosuso.capstone.global.exception.BusinessException;
import com.knu.sosuso.capstone.global.exception.error.TrendingError;
import com.knu.sosuso.capstone.global.service.mapper.ResponseMappingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 인기 영상 관련 Facade 서비스
 * Controller는 이 서비스만 호출
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class PopularVideoService {

    private final PopularityCalculator popularityCalculator;
    private final VideoStatsRepository videoStatsRepository;
    private final VideoRepository videoRepository;
    private final ResponseMappingService responseMappingService;

    private static final int MAX_RESULTS_LIMIT = 30;

    /**
     * 인기 영상 TOP N 조회
     * Caffeine 캐시 적용 (TTL: 60분)
     *
     * N+1 쿼리 해결: IN 쿼리로 한 번에 조회
     */
    @Cacheable(value = "popularVideos", key = "#maxResults", sync = true)
    @Transactional(readOnly = true)
    public List<VideoSummaryResponse> getPopularVideos(String token, int maxResults) {
        log.info("인기 영상 조회 시작: maxResults={}", maxResults);

        // 파라미터 검증
        if (maxResults < 1 || maxResults > MAX_RESULTS_LIMIT) {
            throw new BusinessException(TrendingError.INVALID_MAX_RESULTS);
        }

        try {
            Pageable pageable = PageRequest.of(0, maxResults);
            Page<VideoStats> topStats = videoStatsRepository.findTopByPopularityScore(pageable);

            if (topStats.isEmpty()) {
                log.warn("인기 영상 데이터가 없습니다. 점수 계산이 필요합니다.");
                return new ArrayList<>();
            }

            // 1. apiVideoId 리스트 추출
            List<String> apiVideoIds = topStats.getContent().stream()
                    .map(VideoStats::getApiVideoId)
                    .collect(Collectors.toList());

            // 2. IN 쿼리로 한 번에 조회 (N+1 해결!)
            List<Video> videos = videoRepository.findByApiVideoIdIn(apiVideoIds);

            // 3. Map으로 변환 (빠른 조회용)
            Map<String, Video> videoMap = videos.stream()
                    .collect(Collectors.toMap(
                            Video::getApiVideoId,
                            Function.identity()
                    ));

            // 4. 순서 유지하면서 결과 생성
            List<VideoSummaryResponse> results = new ArrayList<>();

            for (VideoStats stats : topStats) {
                try {
                    Video video = videoMap.get(stats.getApiVideoId());

                    if (video == null) {
                        log.warn("VideoStats에는 있지만 Video가 없음: apiVideoId={}",
                                stats.getApiVideoId());
                        continue;
                    }

                    // DB 데이터를 DTO로 변환
                    VideoSummaryResponse summaryResponse = responseMappingService
                            .mapDbToVideoSummaryResponse(token, video);

                    results.add(summaryResponse);

                } catch (Exception e) {
                    log.error("인기 영상 변환 실패: apiVideoId={}, error={}",
                            stats.getApiVideoId(), e.getMessage(), e);
                }
            }

            log.info("인기 영상 조회 완료: 요청={}개, 성공={}개", maxResults, results.size());
            return results;

        } catch (BusinessException e) {
            throw e;

        } catch (Exception e) {
            log.error("인기 영상 조회 중 예외 발생: {}", e.getMessage(), e);
            throw new BusinessException(TrendingError.TRENDING_PROCESSING_ERROR);
        }
    }

    /**
     * 인기도 점수 재계산 (배치 작업)
     * Scheduler에서 호출
     */
    public int recalculatePopularity() {
        try {
            return popularityCalculator.calculateAndUpdate();

        } catch (Exception e) {
            log.error("인기도 점수 재계산 실패: {}", e.getMessage(), e);
            throw new BusinessException(TrendingError.TRENDING_PROCESSING_ERROR);
        }
    }

    /**
     * 오래된 조회 로그 삭제 (배치 작업)
     * Scheduler에서 호출
     */
    public void cleanupOldLogs() {
        try {
            popularityCalculator.cleanupOldLogs();

        } catch (Exception e) {
            log.error("로그 삭제 실패: {}", e.getMessage(), e);
            throw new BusinessException(TrendingError.TRENDING_PROCESSING_ERROR);
        }
    }
}