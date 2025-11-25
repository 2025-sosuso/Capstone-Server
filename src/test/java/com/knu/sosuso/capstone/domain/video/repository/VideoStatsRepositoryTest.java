package com.knu.sosuso.capstone.domain.video.repository;

import com.knu.sosuso.capstone.domain.video.entity.VideoStats;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("VideoStatsRepository 통합 테스트")
class VideoStatsRepositoryTest {

    @Autowired
    private VideoStatsRepository videoStatsRepository;

    @BeforeEach
    void setUp() {
        videoStatsRepository.deleteAll();
    }

    @Nested
    @DisplayName("findTopByPopularityScore 메서드는")
    class FindTopByPopularityScoreTest {

        @Test
        @DisplayName("인기 점수 순으로 영상을 조회한다")
        void findTopByPopularityScore_OrderByScore_ReturnsDescending() {
            // given
            VideoStats stats1 = createVideoStats("video-1", 100.0);
            VideoStats stats2 = createVideoStats("video-2", 500.0);
            VideoStats stats3 = createVideoStats("video-3", 300.0);
            videoStatsRepository.save(stats1);
            videoStatsRepository.save(stats2);
            videoStatsRepository.save(stats3);

            Pageable pageable = PageRequest.of(0, 10);

            // when
            Page<VideoStats> found = videoStatsRepository.findTopByPopularityScore(pageable);

            // then
            assertThat(found.getContent()).hasSize(3);
            assertThat(found.getContent().get(0).getPopularityScore()).isEqualTo(500.0);
            assertThat(found.getContent().get(1).getPopularityScore()).isEqualTo(300.0);
            assertThat(found.getContent().get(2).getPopularityScore()).isEqualTo(100.0);
        }

        @Test
        @DisplayName("점수가 0보다 큰 영상만 조회한다")
        void findTopByPopularityScore_OnlyPositiveScore_ReturnsFiltered() {
            // given
            VideoStats positive1 = createVideoStats("video-1", 100.0);
            VideoStats positive2 = createVideoStats("video-2", 200.0);
            VideoStats zero = createVideoStats("video-3", 0.0);
            VideoStats negative = createVideoStats("video-4", -10.0);

            videoStatsRepository.save(positive1);
            videoStatsRepository.save(positive2);
            videoStatsRepository.save(zero);
            videoStatsRepository.save(negative);

            Pageable pageable = PageRequest.of(0, 10);

            // when
            Page<VideoStats> found = videoStatsRepository.findTopByPopularityScore(pageable);

            // then
            assertThat(found.getContent()).hasSize(2);
            assertThat(found.getContent()).allMatch(s -> s.getPopularityScore() > 0);
        }

        @Test
        @DisplayName("페이징을 지원한다")
        void findTopByPopularityScore_WithPagination_ReturnsPagedResults() {
            // given
            for (int i = 1; i <= 15; i++) {
                VideoStats stats = createVideoStats("video-" + i, (double) i * 10);
                videoStatsRepository.save(stats);
            }

            Pageable page1 = PageRequest.of(0, 5);
            Pageable page2 = PageRequest.of(1, 5);

            // when
            Page<VideoStats> firstPage = videoStatsRepository.findTopByPopularityScore(page1);
            Page<VideoStats> secondPage = videoStatsRepository.findTopByPopularityScore(page2);

            // then
            assertThat(firstPage.getContent()).hasSize(5);
            assertThat(secondPage.getContent()).hasSize(5);
            assertThat(firstPage.getTotalElements()).isEqualTo(15);
            assertThat(firstPage.getTotalPages()).isEqualTo(3);
        }

        @Test
        @DisplayName("결과가 없으면 빈 페이지를 반환한다")
        void findTopByPopularityScore_NoStats_ReturnsEmpty() {
            // given
            Pageable pageable = PageRequest.of(0, 10);

            // when
            Page<VideoStats> found = videoStatsRepository.findTopByPopularityScore(pageable);

            // then
            assertThat(found.getContent()).isEmpty();
            assertThat(found.getTotalElements()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("save 메서드는")
    class SaveTest {

        @Test
        @DisplayName("새로운 통계를 저장한다")
        void save_NewStats_SavesSuccessfully() {
            // given
            VideoStats stats = createVideoStats("new-video", 123.45);

            // when
            VideoStats saved = videoStatsRepository.save(stats);

            // then
            assertThat(saved.getApiVideoId()).isEqualTo("new-video");
            assertThat(saved.getPopularityScore()).isEqualTo(123.45);
        }

        @Test
        @DisplayName("기존 통계를 업데이트한다")
        void save_ExistingStats_UpdatesSuccessfully() {
            // given
            VideoStats stats = createVideoStats("video-123", 100.0);
            videoStatsRepository.save(stats);

            // when
            stats.setPopularityScore(999.0);
            stats.setViewCountRecent(500);
            stats.setScrapCount(50);
            VideoStats updated = videoStatsRepository.save(stats);

            // then
            VideoStats found = videoStatsRepository.findById("video-123").orElseThrow();
            assertThat(found.getPopularityScore()).isEqualTo(999.0);
            assertThat(found.getViewCountRecent()).isEqualTo(500);
            assertThat(found.getScrapCount()).isEqualTo(50);
        }
    }

    // ========== Helper Methods ==========

    private VideoStats createVideoStats(String apiVideoId, Double popularityScore) {
        VideoStats stats = new VideoStats();
        stats.setApiVideoId(apiVideoId);
        stats.setPopularityScore(popularityScore);
        stats.setViewCountRecent(10);
        stats.setScrapCount(5);
        stats.setLastCalculatedAt(LocalDateTime.now());
        return stats;
    }
}