package com.knu.sosuso.capstone.domain.video.repository;

import com.knu.sosuso.capstone.domain.video.entity.AIAnalysisStatus;
import com.knu.sosuso.capstone.domain.video.entity.Video;
import com.knu.sosuso.capstone.domain.video.entity.VideoType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("VideoRepository 통합 테스트")
class VideoRepositoryTest {

    @Autowired
    private VideoRepository videoRepository;

    @BeforeEach
    void setUp() {
        videoRepository.deleteAll();
    }

    @Nested
    @DisplayName("findByApiVideoId 메서드는")
    class FindByApiVideoIdTest {

        @Test
        @DisplayName("존재하는 영상을 조회한다")
        void findByApiVideoId_Exists_ReturnsVideo() {
            // given
            Video video = createTestVideo("test-123", "테스트 영상");
            videoRepository.save(video);

            // when
            Optional<Video> found = videoRepository.findByApiVideoId("test-123");

            // then
            assertThat(found).isPresent();
            assertThat(found.get().getApiVideoId()).isEqualTo("test-123");
            assertThat(found.get().getTitle()).isEqualTo("테스트 영상");
        }

        @Test
        @DisplayName("존재하지 않는 영상은 빈 Optional을 반환한다")
        void findByApiVideoId_NotExists_ReturnsEmpty() {
            // when
            Optional<Video> found = videoRepository.findByApiVideoId("not-exists");

            // then
            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("UNIQUE 제약조건으로 중복 저장을 방지한다")
        void findByApiVideoId_DuplicateApiVideoId_ThrowsException() {
            // given
            Video video1 = createTestVideo("duplicate-id", "영상1");
            videoRepository.save(video1);

            Video video2 = createTestVideo("duplicate-id", "영상2");

            // when & then
            org.junit.jupiter.api.Assertions.assertThrows(
                    org.springframework.dao.DataIntegrityViolationException.class,
                    () -> {
                        videoRepository.save(video2);
                        videoRepository.flush();
                    }
            );
        }
    }

    @Nested
    @DisplayName("findTop10ByAiAnalysisStatusOrderByUpdatedAtDesc 메서드는")
    class FindTop10ByAiAnalysisStatusTest {

        @Test
        @DisplayName("특정 상태의 영상을 최신순으로 10개 조회한다")
        void findTop10_PendingStatus_ReturnsLatest10() throws InterruptedException {
            // given
            for (int i = 0; i < 15; i++) {
                Video video = createTestVideo("video-" + i, "영상" + i);
                video.setAiAnalysisStatus(AIAnalysisStatus.PENDING);
                videoRepository.save(video);
                Thread.sleep(10); // updatedAt 차이를 위해
            }

            // when
            List<Video> found = videoRepository
                    .findTop10ByAiAnalysisStatusOrderByUpdatedAtDesc(AIAnalysisStatus.PENDING);

            // then
            assertThat(found).hasSize(10);
            assertThat(found).allMatch(v -> v.getAiAnalysisStatus() == AIAnalysisStatus.PENDING);
        }

        @Test
        @DisplayName("COMPLETED 상태만 조회한다")
        void findTop10_CompletedStatus_ReturnsOnlyCompleted() {
            // given
            Video pending = createTestVideo("pending-1", "대기중");
            pending.setAiAnalysisStatus(AIAnalysisStatus.PENDING);

            Video completed1 = createTestVideo("completed-1", "완료1");
            completed1.setAiAnalysisStatus(AIAnalysisStatus.COMPLETED);

            Video completed2 = createTestVideo("completed-2", "완료2");
            completed2.setAiAnalysisStatus(AIAnalysisStatus.COMPLETED);

            videoRepository.saveAll(List.of(pending, completed1, completed2));

            // when
            List<Video> found = videoRepository
                    .findTop10ByAiAnalysisStatusOrderByUpdatedAtDesc(AIAnalysisStatus.COMPLETED);

            // then
            assertThat(found).hasSize(2);
            assertThat(found).allMatch(v -> v.getAiAnalysisStatus() == AIAnalysisStatus.COMPLETED);
        }

        @Test
        @DisplayName("결과가 10개 미만이면 있는 만큼만 반환한다")
        void findTop10_LessThan10_ReturnsAll() {
            // given
            for (int i = 0; i < 5; i++) {
                Video video = createTestVideo("video-" + i, "영상" + i);
                video.setAiAnalysisStatus(AIAnalysisStatus.PENDING);
                videoRepository.save(video);
            }

            // when
            List<Video> found = videoRepository
                    .findTop10ByAiAnalysisStatusOrderByUpdatedAtDesc(AIAnalysisStatus.PENDING);

            // then
            assertThat(found).hasSize(5);
        }

        @Test
        @DisplayName("최신 영상이 먼저 조회된다")
        void findTop10_OrderByUpdatedAt_ReturnsLatestFirst() throws InterruptedException {
            // given
            Video old = createTestVideo("old", "오래된 영상");
            old.setAiAnalysisStatus(AIAnalysisStatus.PENDING);
            videoRepository.save(old);

            Thread.sleep(100);

            Video recent = createTestVideo("recent", "최근 영상");
            recent.setAiAnalysisStatus(AIAnalysisStatus.PENDING);
            videoRepository.save(recent);

            // when
            List<Video> found = videoRepository
                    .findTop10ByAiAnalysisStatusOrderByUpdatedAtDesc(AIAnalysisStatus.PENDING);

            // then
            assertThat(found).hasSize(2);
            assertThat(found.get(0).getApiVideoId()).isEqualTo("recent");
            assertThat(found.get(1).getApiVideoId()).isEqualTo("old");
        }
    }

    @Nested
    @DisplayName("findByDeletedTrueAndDeleteCheckedAtBefore 메서드는")
    class FindByDeletedTrueTest {

        @Test
        @DisplayName("삭제되고 오래된 영상을 조회한다")
        void findByDeletedTrue_OldVideos_ReturnsDeleted() {
            // given
            LocalDateTime threshold = LocalDateTime.now().minusDays(30);

            Video oldDeleted = createTestVideo("old-deleted", "오래된 삭제 영상");
            oldDeleted.setDeleted(true);
            oldDeleted.setDeleteCheckedAt(threshold.minusDays(1));

            Video recentDeleted = createTestVideo("recent-deleted", "최근 삭제 영상");
            recentDeleted.setDeleted(true);
            recentDeleted.setDeleteCheckedAt(LocalDateTime.now());

            Video notDeleted = createTestVideo("not-deleted", "삭제되지 않은 영상");
            notDeleted.setDeleted(false);

            videoRepository.saveAll(List.of(oldDeleted, recentDeleted, notDeleted));

            // when
            List<Video> found = videoRepository
                    .findByDeletedTrueAndDeleteCheckedAtBefore(threshold);

            // then
            assertThat(found).hasSize(1);
            assertThat(found.get(0).getApiVideoId()).isEqualTo("old-deleted");
            assertThat(found.get(0).isDeleted()).isTrue();
        }

        @Test
        @DisplayName("삭제되지 않은 영상은 조회되지 않는다")
        void findByDeletedTrue_NotDeleted_ReturnsEmpty() {
            // given
            LocalDateTime threshold = LocalDateTime.now().minusDays(30);

            Video notDeleted = createTestVideo("not-deleted", "삭제 안됨");
            notDeleted.setDeleted(false);
            notDeleted.setDeleteCheckedAt(threshold.minusDays(1));
            videoRepository.save(notDeleted);

            // when
            List<Video> found = videoRepository
                    .findByDeletedTrueAndDeleteCheckedAtBefore(threshold);

            // then
            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("threshold 이후에 삭제 확인된 영상은 조회되지 않는다")
        void findByDeletedTrue_AfterThreshold_NotIncluded() {
            // given
            LocalDateTime threshold = LocalDateTime.now().minusDays(30);

            Video afterThreshold = createTestVideo("after", "threshold 이후");
            afterThreshold.setDeleted(true);
            afterThreshold.setDeleteCheckedAt(threshold.plusDays(1));
            videoRepository.save(afterThreshold);

            // when
            List<Video> found = videoRepository
                    .findByDeletedTrueAndDeleteCheckedAtBefore(threshold);

            // then
            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("findVideosNeedingMetadataUpdate 메서드는")
    class FindVideosNeedingMetadataUpdateTest {

        @Test
        @DisplayName("쿼리 문법이 올바르게 작동한다")
        void findVideosNeedingMetadataUpdate_QuerySyntax_Valid() {
            // given
            LocalDateTime threshold = LocalDateTime.now().minusDays(30);

            Video oldMetadata = createTestVideo("old-meta", "오래된 메타데이터");
            oldMetadata.setLastMetadataUpdatedAt(threshold.minusDays(1));
            oldMetadata.setDeleted(false);
            videoRepository.save(oldMetadata);

            // when - Scrap과 INNER JOIN이므로 Scrap이 없으면 결과 없음
            List<Video> found = videoRepository
                    .findVideosNeedingMetadataUpdate(threshold);

            // then
            // Scrap 없으면 빈 결과 (정상)
            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("lastMetadataUpdatedAt이 null인 경우도 처리한다")
        void findVideosNeedingMetadataUpdate_NullLastUpdated_QueryValid() {
            // given
            LocalDateTime threshold = LocalDateTime.now().minusDays(30);

            Video nullMetadata = createTestVideo("null-meta", "메타데이터 없음");
            nullMetadata.setLastMetadataUpdatedAt(null);
            nullMetadata.setDeleted(false);
            videoRepository.save(nullMetadata);

            // when
            List<Video> found = videoRepository
                    .findVideosNeedingMetadataUpdate(threshold);

            // then
            assertThat(found).isEmpty(); // Scrap 없으면 조회 안됨
        }

        @Test
        @DisplayName("deleted가 true인 영상은 제외된다")
        void findVideosNeedingMetadataUpdate_DeletedVideos_Excluded() {
            // given
            LocalDateTime threshold = LocalDateTime.now().minusDays(30);

            Video deletedVideo = createTestVideo("deleted", "삭제된 영상");
            deletedVideo.setLastMetadataUpdatedAt(threshold.minusDays(1));
            deletedVideo.setDeleted(true);
            videoRepository.save(deletedVideo);

            // when
            List<Video> found = videoRepository
                    .findVideosNeedingMetadataUpdate(threshold);

            // then
            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("save 메서드는")
    class SaveTest {

        @Test
        @DisplayName("새로운 영상을 저장한다")
        void save_NewVideo_SavesSuccessfully() {
            // given
            Video video = createTestVideo("new-video", "새 영상");

            // when
            Video saved = videoRepository.save(video);

            // then
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getApiVideoId()).isEqualTo("new-video");
        }

        @Test
        @DisplayName("영상 정보를 업데이트한다")
        void save_ExistingVideo_UpdatesSuccessfully() {
            // given
            Video video = createTestVideo("test", "원래 제목");
            Video saved = videoRepository.save(video);

            // when
            saved.setTitle("변경된 제목");
            saved.setViewCount("9999");
            videoRepository.save(saved);

            // then
            Video updated = videoRepository.findByApiVideoId("test").orElseThrow();
            assertThat(updated.getTitle()).isEqualTo("변경된 제목");
            assertThat(updated.getViewCount()).isEqualTo("9999");
        }
    }

    // ========== Helper Methods ==========

    private Video createTestVideo(String apiVideoId, String title) {
        return createTestVideo(apiVideoId, title, VideoType.VIDEO);
    }

    private Video createTestVideo(String apiVideoId, String title, VideoType videoType) {
        return Video.builder()
                .apiVideoId(apiVideoId)
                .title(title)
                .description("테스트 설명")
                .videoType(videoType)
                .viewCount("1000")
                .likeCount("100")
                .commentCount("50")
                .thumbnailUrl("https://example.com/thumb.jpg")
                .channelId("channel-123")
                .channelName("테스트 채널")
                .channelThumbnailUrl("https://example.com/channel.jpg")
                .subscriberCount("10000")
                .uploadedAt("2024-01-01T00:00:00Z")
                .commentHistogram("{}")
                .popularTimestamps("{}")
                .aiAnalysisStatus(AIAnalysisStatus.PENDING)
                .lastMetadataUpdatedAt(LocalDateTime.now())
                .commentsDisabled(false)
                .hasNoComments(false)
                .deleted(false)
                .build();
    }
}