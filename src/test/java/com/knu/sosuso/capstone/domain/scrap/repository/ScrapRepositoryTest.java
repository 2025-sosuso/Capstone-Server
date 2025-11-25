package com.knu.sosuso.capstone.domain.scrap.repository;

import com.knu.sosuso.capstone.domain.auth.User;
import com.knu.sosuso.capstone.domain.auth.UserRepository;
import com.knu.sosuso.capstone.domain.scrap.entity.Scrap;
import com.knu.sosuso.capstone.domain.video.entity.AIAnalysisStatus;
import com.knu.sosuso.capstone.domain.video.entity.Video;
import com.knu.sosuso.capstone.domain.video.entity.VideoType;
import com.knu.sosuso.capstone.domain.video.repository.VideoRepository;
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
@DisplayName("ScrapRepository 통합 테스트")
class ScrapRepositoryTest {

    @Autowired
    private ScrapRepository scrapRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VideoRepository videoRepository;

    private User testUser;
    private Video testVideo;

    @BeforeEach
    void setUp() {
        scrapRepository.deleteAll();
        videoRepository.deleteAll();
        userRepository.deleteAll();

        testUser = createAndSaveUser("test-sub-123", "테스트유저");
        testVideo = createAndSaveVideo("test-video-123", "테스트영상");
    }

    @Nested
    @DisplayName("existsByVideoId 메서드는")
    class ExistsByVideoIdTest {

        @Test
        @DisplayName("스크랩이 존재하면 true를 반환한다")
        void existsByVideoId_Exists_ReturnsTrue() {
            // given
            Scrap scrap = createScrap(testUser, testVideo);
            scrapRepository.save(scrap);

            // when
            boolean exists = scrapRepository.existsByVideoId(testVideo.getId());

            // then
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("스크랩이 없으면 false를 반환한다")
        void existsByVideoId_NotExists_ReturnsFalse() {
            // when
            boolean exists = scrapRepository.existsByVideoId(999L);

            // then
            assertThat(exists).isFalse();
        }

        @Test
        @DisplayName("여러 사용자가 스크랩해도 true를 반환한다")
        void existsByVideoId_MultipleUsers_ReturnsTrue() {
            // given
            User user1 = createAndSaveUser("user1", "유저1");
            User user2 = createAndSaveUser("user2", "유저2");

            scrapRepository.save(createScrap(user1, testVideo));
            scrapRepository.save(createScrap(user2, testVideo));

            // when
            boolean exists = scrapRepository.existsByVideoId(testVideo.getId());

            // then
            assertThat(exists).isTrue();
        }
    }

    @Nested
    @DisplayName("existsByUserIdAndApiVideoId 메서드는")
    class ExistsByUserIdAndApiVideoIdTest {

        @Test
        @DisplayName("사용자가 해당 영상을 스크랩했으면 true를 반환한다")
        void existsByUserAndVideo_Exists_ReturnsTrue() {
            // given
            Scrap scrap = createScrap(testUser, testVideo);
            scrapRepository.save(scrap);

            // when
            boolean exists = scrapRepository.existsByUserIdAndApiVideoId(
                    testUser.getId(),
                    testVideo.getApiVideoId()
            );

            // then
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("스크랩하지 않았으면 false를 반환한다")
        void existsByUserAndVideo_NotExists_ReturnsFalse() {
            // when
            boolean exists = scrapRepository.existsByUserIdAndApiVideoId(
                    testUser.getId(),
                    "not-scraped-video"
            );

            // then
            assertThat(exists).isFalse();
        }

        @Test
        @DisplayName("다른 사용자의 스크랩은 false를 반환한다")
        void existsByUserAndVideo_OtherUser_ReturnsFalse() {
            // given
            User otherUser = createAndSaveUser("other", "다른유저");
            scrapRepository.save(createScrap(otherUser, testVideo));

            // when
            boolean exists = scrapRepository.existsByUserIdAndApiVideoId(
                    testUser.getId(),
                    testVideo.getApiVideoId()
            );

            // then
            assertThat(exists).isFalse();
        }
    }

    @Nested
    @DisplayName("findByUserIdAndApiVideoId 메서드는")
    class FindByUserIdAndApiVideoIdTest {

        @Test
        @DisplayName("사용자의 스크랩을 조회한다")
        void findByUserAndVideo_Exists_ReturnsScrap() {
            // given
            Scrap scrap = createScrap(testUser, testVideo);
            scrapRepository.save(scrap);

            // when
            Optional<Scrap> found = scrapRepository.findByUserIdAndApiVideoId(
                    testUser.getId(),
                    testVideo.getApiVideoId()
            );

            // then
            assertThat(found).isPresent();
            assertThat(found.get().getUser()).isEqualTo(testUser);
            assertThat(found.get().getVideo()).isEqualTo(testVideo);
            assertThat(found.get().getApiVideoId()).isEqualTo(testVideo.getApiVideoId());
        }

        @Test
        @DisplayName("스크랩이 없으면 빈 Optional을 반환한다")
        void findByUserAndVideo_NotExists_ReturnsEmpty() {
            // when
            Optional<Scrap> found = scrapRepository.findByUserIdAndApiVideoId(
                    testUser.getId(),
                    "not-scraped"
            );

            // then
            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("다른 사용자의 스크랩은 조회되지 않는다")
        void findByUserAndVideo_OtherUser_ReturnsEmpty() {
            // given
            User otherUser = createAndSaveUser("other", "다른유저");
            scrapRepository.save(createScrap(otherUser, testVideo));

            // when
            Optional<Scrap> found = scrapRepository.findByUserIdAndApiVideoId(
                    testUser.getId(),
                    testVideo.getApiVideoId()
            );

            // then
            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByUserIdOrderByCreatedAtDesc 메서드는")
    class FindByUserIdOrderByCreatedAtDescTest {

        @Test
        @DisplayName("사용자의 스크랩 목록을 최신순으로 조회한다")
        void findByUser_OrderByCreatedAt_ReturnsDescending() throws InterruptedException {
            // given
            Video video1 = createAndSaveVideo("video-1", "영상1");
            Video video2 = createAndSaveVideo("video-2", "영상2");
            Video video3 = createAndSaveVideo("video-3", "영상3");

            scrapRepository.save(createScrap(testUser, video1));
            Thread.sleep(10);
            scrapRepository.save(createScrap(testUser, video2));
            Thread.sleep(10);
            scrapRepository.save(createScrap(testUser, video3));

            // when
            List<Scrap> found = scrapRepository.findByUserIdOrderByCreatedAtDesc(testUser.getId());

            // then
            assertThat(found).hasSize(3);
            assertThat(found.get(0).getVideo()).isEqualTo(video3); // 가장 최근
            assertThat(found.get(2).getVideo()).isEqualTo(video1); // 가장 오래됨
        }

        @Test
        @DisplayName("스크랩이 없으면 빈 리스트를 반환한다")
        void findByUser_NoScraps_ReturnsEmpty() {
            // when
            List<Scrap> found = scrapRepository.findByUserIdOrderByCreatedAtDesc(testUser.getId());

            // then
            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("다른 사용자의 스크랩은 조회되지 않는다")
        void findByUser_OtherUserScraps_NotIncluded() {
            // given
            User otherUser = createAndSaveUser("other-sub", "다른유저");
            scrapRepository.save(createScrap(otherUser, testVideo));

            // when
            List<Scrap> found = scrapRepository.findByUserIdOrderByCreatedAtDesc(testUser.getId());

            // then
            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("여러 영상을 스크랩해도 모두 조회된다")
        void findByUser_MultipleVideos_ReturnsAll() {
            // given
            Video video1 = createAndSaveVideo("video-1", "영상1");
            Video video2 = createAndSaveVideo("video-2", "영상2");
            Video video3 = createAndSaveVideo("video-3", "영상3");

            scrapRepository.save(createScrap(testUser, video1));
            scrapRepository.save(createScrap(testUser, video2));
            scrapRepository.save(createScrap(testUser, video3));

            // when
            List<Scrap> found = scrapRepository.findByUserIdOrderByCreatedAtDesc(testUser.getId());

            // then
            assertThat(found).hasSize(3);
        }
    }

    @Nested
    @DisplayName("countScrapsByVideo 메서드는")
    class CountScrapsByVideoTest {

        @Test
        @DisplayName("영상별 스크랩 횟수를 집계한다")
        void countScrapsByVideo_MultipleVideos_ReturnsCount() {
            // given
            Video video1 = createAndSaveVideo("video-1", "영상1");
            Video video2 = createAndSaveVideo("video-2", "영상2");

            User user1 = createAndSaveUser("user1", "유저1");
            User user2 = createAndSaveUser("user2", "유저2");
            User user3 = createAndSaveUser("user3", "유저3");

            // video1: 2회 스크랩
            scrapRepository.save(createScrap(user1, video1));
            scrapRepository.save(createScrap(user2, video1));

            // video2: 1회 스크랩
            scrapRepository.save(createScrap(user3, video2));

            // when
            List<Object[]> results = scrapRepository.countScrapsByVideo();

            // then
            assertThat(results).hasSize(2);
            // 결과는 (apiVideoId, count) 형태

            // video1 검증
            Object[] video1Result = results.stream()
                    .filter(r -> r[0].equals("video-1"))
                    .findFirst()
                    .orElseThrow();
            assertThat(((Number) video1Result[1]).longValue()).isEqualTo(2L);

            // video2 검증
            Object[] video2Result = results.stream()
                    .filter(r -> r[0].equals("video-2"))
                    .findFirst()
                    .orElseThrow();
            assertThat(((Number) video2Result[1]).longValue()).isEqualTo(1L);
        }

        @Test
        @DisplayName("스크랩이 없으면 빈 리스트를 반환한다")
        void countScrapsByVideo_NoScraps_ReturnsEmpty() {
            // when
            List<Object[]> results = scrapRepository.countScrapsByVideo();

            // then
            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("같은 사용자가 여러 영상을 스크랩해도 정확히 집계한다")
        void countScrapsByVideo_SameUserMultipleVideos_CountsSeparately() {
            // given
            Video video1 = createAndSaveVideo("video-1", "영상1");
            Video video2 = createAndSaveVideo("video-2", "영상2");

            scrapRepository.save(createScrap(testUser, video1));
            scrapRepository.save(createScrap(testUser, video2));

            // when
            List<Object[]> results = scrapRepository.countScrapsByVideo();

            // then
            assertThat(results).hasSize(2);
            // 각 영상마다 1회씩
            assertThat(results).allMatch(r -> ((Number) r[1]).longValue() == 1L);
        }
    }

    @Nested
    @DisplayName("findByUserIdAndVideoId 메서드는")
    class FindByUserIdAndVideoIdTest {

        @Test
        @DisplayName("사용자 ID와 비디오 ID로 스크랩을 조회한다")
        void findByUserIdAndVideoId_Exists_ReturnsScrap() {
            // given
            Scrap scrap = createScrap(testUser, testVideo);
            scrapRepository.save(scrap);

            // when
            Optional<Scrap> found = scrapRepository.findByUserIdAndVideoId(
                    testUser.getId(),
                    testVideo.getId()
            );

            // then
            assertThat(found).isPresent();
            assertThat(found.get().getUser()).isEqualTo(testUser);
            assertThat(found.get().getVideo()).isEqualTo(testVideo);
        }

        @Test
        @DisplayName("존재하지 않으면 빈 Optional을 반환한다")
        void findByUserIdAndVideoId_NotExists_ReturnsEmpty() {
            // when
            Optional<Scrap> found = scrapRepository.findByUserIdAndVideoId(
                    testUser.getId(),
                    999L
            );

            // then
            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("다른 사용자의 스크랩은 조회되지 않는다")
        void findByUserIdAndVideoId_OtherUser_ReturnsEmpty() {
            // given
            User otherUser = createAndSaveUser("other", "다른유저");
            scrapRepository.save(createScrap(otherUser, testVideo));

            // when
            Optional<Scrap> found = scrapRepository.findByUserIdAndVideoId(
                    testUser.getId(),
                    testVideo.getId()
            );

            // then
            assertThat(found).isEmpty();
        }
    }

    // ========== Helper Methods ==========

    private User createAndSaveUser(String sub, String name) {
        User user = User.builder()
                .sub(sub)
                .email(sub + "@example.com")
                .name(name)
                .role("ROLE_USER")
                .picture("https://example.com/picture.jpg")
                .build();
        return userRepository.save(user);
    }

    private Video createAndSaveVideo(String apiVideoId, String title) {
        Video video = Video.builder()
                .apiVideoId(apiVideoId)
                .title(title)
                .description("테스트 설명")
                .videoType(VideoType.VIDEO)
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
                .build();
        return videoRepository.save(video);
    }

    private Scrap createScrap(User user, Video video) {
        return Scrap.builder()
                .user(user)
                .video(video)
                .apiVideoId(video.getApiVideoId())
                .build();
    }
}