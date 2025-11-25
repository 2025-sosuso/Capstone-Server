package com.knu.sosuso.capstone.domain.video.repository;

import com.knu.sosuso.capstone.domain.video.entity.VideoViewLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("VideoViewLogRepository 통합 테스트")
class VideoViewLogRepositoryTest {

    @Autowired
    private VideoViewLogRepository videoViewLogRepository;

    @BeforeEach
    void setUp() {
        videoViewLogRepository.deleteAll();
    }

    @Nested
    @DisplayName("countViewsByVideoSince 메서드는")
    class CountViewsByVideoSinceTest {

        @Test
        @DisplayName("특정 기간 이후의 조회수를 집계한다")
        void countViewsSince_WithinPeriod_ReturnsCount() {
            // given
            LocalDateTime since = LocalDateTime.now().minusDays(7);

            // video-1: 3회 조회
            saveViewLog("video-1", 1L, since.plusDays(1));
            saveViewLog("video-1", 2L, since.plusDays(2));
            saveViewLog("video-1", 3L, since.plusDays(3));

            // video-2: 2회 조회
            saveViewLog("video-2", 1L, since.plusDays(1));
            saveViewLog("video-2", 2L, since.plusDays(2));

            // when
            List<Object[]> results = videoViewLogRepository.countViewsByVideoSince(since);

            // then
            assertThat(results).hasSize(2);
            // 결과는 [apiVideoId, count] 형태
        }

        @Test
        @DisplayName("기간 이전의 로그는 집계하지 않는다")
        void countViewsSince_BeforePeriod_NotIncluded() {
            // given
            LocalDateTime since = LocalDateTime.now().minusDays(7);

            // 기간 이후
            saveViewLog("video-1", 1L, since.plusDays(1));

            // 기간 이전
            saveViewLog("video-1", 2L, since.minusDays(1));

            // when
            List<Object[]> results = videoViewLogRepository.countViewsByVideoSince(since);

            // then
            assertThat(results).hasSize(1);
            // video-1의 count는 1이어야 함
        }

        @Test
        @DisplayName("로그가 없으면 빈 리스트를 반환한다")
        void countViewsSince_NoLogs_ReturnsEmpty() {
            // given
            LocalDateTime since = LocalDateTime.now().minusDays(7);

            // when
            List<Object[]> results = videoViewLogRepository.countViewsByVideoSince(since);

            // then
            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("동일 영상의 여러 조회를 정확히 집계한다")
        void countViewsSince_MultipleViews_CountsAll() {
            // given
            LocalDateTime since = LocalDateTime.now().minusDays(7);
            String videoId = "popular-video";

            // 10회 조회
            for (int i = 1; i <= 10; i++) {
                saveViewLog(videoId, (long) i, since.plusHours(i));
            }

            // when
            List<Object[]> results = videoViewLogRepository.countViewsByVideoSince(since);

            // then
            assertThat(results).hasSize(1);
            Object[] result = results.get(0);
            assertThat(result[0]).isEqualTo(videoId);
            assertThat(((Number) result[1]).longValue()).isEqualTo(10L);
        }
    }

    @Nested
    @DisplayName("deleteOldLogs 메서드는")
    class DeleteOldLogsTest {

        @Test
        @DisplayName("기준일 이전의 오래된 로그를 삭제한다")
        void deleteOldLogs_BeforeDate_DeletesOldLogs() {
            // given
            LocalDateTime threshold = LocalDateTime.now().minusDays(7);

            // 오래된 로그
            saveViewLog("video-1", 1L, threshold.minusDays(1));
            saveViewLog("video-1", 2L, threshold.minusDays(2));

            // 최근 로그
            saveViewLog("video-2", 3L, threshold.plusDays(1));

            // when
            videoViewLogRepository.deleteOldLogs(threshold);
            videoViewLogRepository.flush();

            // then
            List<VideoViewLog> remaining = videoViewLogRepository.findAll();
            assertThat(remaining).hasSize(1);
            assertThat(remaining.get(0).getApiVideoId()).isEqualTo("video-2");
        }

        @Test
        @DisplayName("기준일 정확히 일치하는 로그는 삭제하지 않는다")
        void deleteOldLogs_ExactDate_NotDeleted() {
            // given
            LocalDateTime threshold = LocalDateTime.now().minusDays(7);

            // 정확히 threshold 시간
            saveViewLog("video-1", 1L, threshold);

            // when
            videoViewLogRepository.deleteOldLogs(threshold);
            videoViewLogRepository.flush();

            // then
            List<VideoViewLog> remaining = videoViewLogRepository.findAll();
            assertThat(remaining).hasSize(1);
        }

        @Test
        @DisplayName("삭제할 로그가 없으면 아무 것도 삭제하지 않는다")
        void deleteOldLogs_NoOldLogs_NoChange() {
            // given
            LocalDateTime threshold = LocalDateTime.now().minusDays(7);
            saveViewLog("video-1", 1L, threshold.plusDays(1));

            // when
            videoViewLogRepository.deleteOldLogs(threshold);
            videoViewLogRepository.flush();

            // then
            List<VideoViewLog> remaining = videoViewLogRepository.findAll();
            assertThat(remaining).hasSize(1);
        }

        @Test
        @DisplayName("모든 로그가 오래되었으면 전부 삭제한다")
        void deleteOldLogs_AllOld_DeletesAll() {
            // given
            LocalDateTime threshold = LocalDateTime.now().minusDays(7);

            saveViewLog("video-1", 1L, threshold.minusDays(10));
            saveViewLog("video-2", 2L, threshold.minusDays(20));
            saveViewLog("video-3", 3L, threshold.minusDays(30));

            // when
            videoViewLogRepository.deleteOldLogs(threshold);
            videoViewLogRepository.flush();

            // then
            List<VideoViewLog> remaining = videoViewLogRepository.findAll();
            assertThat(remaining).isEmpty();
        }
    }

    @Nested
    @DisplayName("save 메서드는")
    class SaveTest {

        @Test
        @DisplayName("새로운 조회 로그를 저장한다")
        void save_NewLog_SavesSuccessfully() {
            // given
            VideoViewLog log = VideoViewLog.builder()
                    .apiVideoId("test-video")
                    .userId(1L)
                    .viewedAt(LocalDateTime.now())
                    .build();

            // when
            VideoViewLog saved = videoViewLogRepository.save(log);

            // then
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getApiVideoId()).isEqualTo("test-video");
            assertThat(saved.getUserId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("userId가 null이어도 저장된다 (비로그인 사용자)")
        void save_NullUserId_SavesSuccessfully() {
            // given
            VideoViewLog log = VideoViewLog.builder()
                    .apiVideoId("test-video")
                    .userId(null)
                    .viewedAt(LocalDateTime.now())
                    .build();

            // when
            VideoViewLog saved = videoViewLogRepository.save(log);

            // then
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getUserId()).isNull();
        }
    }

    // ========== Helper Methods ==========

    private void saveViewLog(String apiVideoId, Long userId, LocalDateTime viewedAt) {
        VideoViewLog log = VideoViewLog.builder()
                .apiVideoId(apiVideoId)
                .userId(userId)
                .viewedAt(viewedAt)
                .build();
        videoViewLogRepository.save(log);
    }
}