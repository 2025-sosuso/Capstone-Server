package com.knu.sosuso.capstone.domain.comment.repository;

import com.knu.sosuso.capstone.domain.comment.entity.Comment;
import com.knu.sosuso.capstone.domain.comment.entity.value.DetailSentimentType;
import com.knu.sosuso.capstone.domain.comment.entity.value.SentimentType;
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
@DisplayName("CommentRepository 통합 테스트")
class CommentRepositoryTest {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private VideoRepository videoRepository;

    private Video testVideo;

    @BeforeEach
    void setUp() {
        commentRepository.deleteAll();
        videoRepository.deleteAll();

        testVideo = createAndSaveVideo("test-video-123", "테스트 영상");
    }

    @Nested
    @DisplayName("findByVideo 메서드는")
    class FindByVideoTest {

        @Test
        @DisplayName("특정 비디오의 모든 댓글을 조회한다")
        void findByVideo_WithComments_ReturnsAll() {
            // given
            Comment comment1 = createComment(testVideo, "api-1", "댓글1");
            Comment comment2 = createComment(testVideo, "api-2", "댓글2");
            Comment comment3 = createComment(testVideo, "api-3", "댓글3");
            commentRepository.saveAll(List.of(comment1, comment2, comment3));

            // when
            List<Comment> found = commentRepository.findByVideo(testVideo);

            // then
            assertThat(found).hasSize(3);
            assertThat(found).extracting(Comment::getApiCommentId)
                    .containsExactlyInAnyOrder("api-1", "api-2", "api-3");
        }

        @Test
        @DisplayName("댓글이 없으면 빈 리스트를 반환한다")
        void findByVideo_NoComments_ReturnsEmpty() {
            // when
            List<Comment> found = commentRepository.findByVideo(testVideo);

            // then
            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("다른 비디오의 댓글은 조회되지 않는다")
        void findByVideo_OtherVideo_NotIncluded() {
            // given
            Video otherVideo = createAndSaveVideo("other-video", "다른 영상");
            Comment otherComment = createComment(otherVideo, "other-api", "다른 댓글");
            commentRepository.save(otherComment);

            // when
            List<Comment> found = commentRepository.findByVideo(testVideo);

            // then
            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByVideoId 메서드는")
    class FindByVideoIdTest {

        @Test
        @DisplayName("비디오 ID로 댓글을 조회한다")
        void findByVideoId_WithComments_ReturnsAll() {
            // given
            Comment comment1 = createComment(testVideo, "api-1", "댓글1");
            Comment comment2 = createComment(testVideo, "api-2", "댓글2");
            commentRepository.saveAll(List.of(comment1, comment2));

            // when
            List<Comment> found = commentRepository.findByVideoId(testVideo.getId());

            // then
            assertThat(found).hasSize(2);
        }

        @Test
        @DisplayName("댓글이 없으면 빈 리스트를 반환한다")
        void findByVideoId_NoComments_ReturnsEmpty() {
            // when
            List<Comment> found = commentRepository.findByVideoId(testVideo.getId());

            // then
            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("existsByApiCommentId 메서드는")
    class ExistsByApiCommentIdTest {

        @Test
        @DisplayName("이미 존재하는 댓글이면 true를 반환한다")
        void existsByApiCommentId_Exists_ReturnsTrue() {
            // given
            Comment comment = createComment(testVideo, "exists-123", "존재하는 댓글");
            commentRepository.save(comment);

            // when
            boolean exists = commentRepository.existsByApiCommentId("exists-123");

            // then
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("존재하지 않는 댓글이면 false를 반환한다")
        void existsByApiCommentId_NotExists_ReturnsFalse() {
            // when
            boolean exists = commentRepository.existsByApiCommentId("not-exists");

            // then
            assertThat(exists).isFalse();
        }
    }

    @Nested
    @DisplayName("findByApiCommentId 메서드는")
    class FindByApiCommentIdTest {

        @Test
        @DisplayName("API 댓글 ID로 댓글을 조회한다")
        void findByApiCommentId_Exists_ReturnsComment() {
            // given
            Comment comment = createComment(testVideo, "api-123", "테스트 댓글");
            commentRepository.save(comment);

            // when
            Optional<Comment> found = commentRepository.findByApiCommentId("api-123");

            // then
            assertThat(found).isPresent();
            assertThat(found.get().getCommentContent()).isEqualTo("테스트 댓글");
        }

        @Test
        @DisplayName("존재하지 않으면 빈 Optional을 반환한다")
        void findByApiCommentId_NotExists_ReturnsEmpty() {
            // when
            Optional<Comment> found = commentRepository.findByApiCommentId("not-exists");

            // then
            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByVideoIdAndTextContaining 메서드는")
    class FindByVideoIdAndTextContainingTest {

        @Test
        @DisplayName("텍스트를 포함하는 댓글을 검색한다")
        void findByText_WithMatchingComments_ReturnsMatched() {
            // given
            Comment comment1 = createComment(testVideo, "api-1", "이 영상 정말 재미있어요");
            Comment comment2 = createComment(testVideo, "api-2", "재미있는 내용이네요");
            Comment comment3 = createComment(testVideo, "api-3", "그냥 그래요");
            commentRepository.saveAll(List.of(comment1, comment2, comment3));

            // when
            List<Comment> found = commentRepository.findByVideoIdAndTextContaining(
                    testVideo.getId(), "재미있"
            );

            // then
            assertThat(found).hasSize(2);
            assertThat(found).extracting(Comment::getCommentContent)
                    .allMatch(content -> content.contains("재미있"));
        }

        @Test
        @DisplayName("대소문자 구분 없이 검색한다")
        void findByText_CaseInsensitive_ReturnsMatched() {
            // given
            Comment comment = createComment(testVideo, "api-1", "GOOD VIDEO");
            commentRepository.save(comment);

            // when
            List<Comment> found = commentRepository.findByVideoIdAndTextContaining(
                    testVideo.getId(), "good"
            );

            // then
            assertThat(found).hasSize(1);
        }

        @Test
        @DisplayName("매칭되는 댓글이 없으면 빈 리스트를 반환한다")
        void findByText_NoMatch_ReturnsEmpty() {
            // given
            Comment comment = createComment(testVideo, "api-1", "평범한 댓글");
            commentRepository.save(comment);

            // when
            List<Comment> found = commentRepository.findByVideoIdAndTextContaining(
                    testVideo.getId(), "재미있"
            );

            // then
            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("부분 문자열도 검색된다")
        void findByText_PartialMatch_ReturnsMatched() {
            // given
            Comment comment = createComment(testVideo, "api-1", "대한민국 화이팅");
            commentRepository.save(comment);

            // when
            List<Comment> found = commentRepository.findByVideoIdAndTextContaining(
                    testVideo.getId(), "민국"
            );

            // then
            assertThat(found).hasSize(1);
        }
    }

    @Nested
    @DisplayName("findByVideoIdAndSentimentTypeOrderById 메서드는")
    class FindByVideoIdAndSentimentTypeTest {

        @Test
        @DisplayName("특정 감정 타입의 댓글만 조회한다")
        void findBySentimentType_Positive_ReturnsOnlyPositive() {
            // given
            Comment positive1 = createComment(testVideo, "api-1", "좋아요", SentimentType.POSITIVE);
            Comment positive2 = createComment(testVideo, "api-2", "최고예요", SentimentType.POSITIVE);
            Comment negative = createComment(testVideo, "api-3", "별로예요", SentimentType.NEGATIVE);
            commentRepository.saveAll(List.of(positive1, positive2, negative));

            // when
            List<Comment> found = commentRepository.findByVideoIdAndSentimentTypeOrderById(
                    testVideo.getId(), SentimentType.POSITIVE
            );

            // then
            assertThat(found).hasSize(2);
            assertThat(found).allMatch(c -> c.getSentimentType() == SentimentType.POSITIVE);
        }

        @Test
        @DisplayName("NEGATIVE 감정만 필터링한다")
        void findBySentimentType_Negative_ReturnsOnlyNegative() {
            // given
            Comment positive = createComment(testVideo, "api-1", "좋아요", SentimentType.POSITIVE);
            Comment negative = createComment(testVideo, "api-2", "싫어요", SentimentType.NEGATIVE);
            commentRepository.saveAll(List.of(positive, negative));

            // when
            List<Comment> found = commentRepository.findByVideoIdAndSentimentTypeOrderById(
                    testVideo.getId(), SentimentType.NEGATIVE
            );

            // then
            assertThat(found).hasSize(1);
            assertThat(found.get(0).getSentimentType()).isEqualTo(SentimentType.NEGATIVE);
        }

        @Test
        @DisplayName("ID 순으로 정렬된다")
        void findBySentimentType_OrderById_ReturnsSorted() {
            // given
            for (int i = 5; i >= 1; i--) {
                Comment comment = createComment(testVideo, "api-" + i, "댓글" + i, SentimentType.POSITIVE);
                commentRepository.save(comment);
            }

            // when
            List<Comment> found = commentRepository.findByVideoIdAndSentimentTypeOrderById(
                    testVideo.getId(), SentimentType.POSITIVE
            );

            // then
            assertThat(found).hasSize(5);
            // ID가 오름차순인지 확인
            for (int i = 0; i < found.size() - 1; i++) {
                assertThat(found.get(i).getId()).isLessThan(found.get(i + 1).getId());
            }
        }

        @Test
        @DisplayName("OTHER 타입도 조회된다")
        void findBySentimentType_Other_ReturnsOther() {
            // given
            Comment other = createComment(testVideo, "api-1", "그냥그래요", SentimentType.OTHER);
            commentRepository.save(other);

            // when
            List<Comment> found = commentRepository.findByVideoIdAndSentimentTypeOrderById(
                    testVideo.getId(), SentimentType.OTHER
            );

            // then
            assertThat(found).hasSize(1);
            assertThat(found.get(0).getSentimentType()).isEqualTo(SentimentType.OTHER);
        }
    }

    @Nested
    @DisplayName("findByVideoIdOrderByLikeCountDesc 메서드는")
    class FindByVideoIdOrderByLikeCountDescTest {

        @Test
        @DisplayName("좋아요 수 내림차순으로 정렬한다")
        void orderByLikeCount_Desc_ReturnsSorted() {
            // given
            Comment comment1 = createComment(testVideo, "api-1", "댓글1");
            comment1.setLikeCount(100);

            Comment comment2 = createComment(testVideo, "api-2", "댓글2");
            comment2.setLikeCount(500);

            Comment comment3 = createComment(testVideo, "api-3", "댓글3");
            comment3.setLikeCount(200);

            commentRepository.saveAll(List.of(comment1, comment2, comment3));

            // when
            List<Comment> found = commentRepository.findByVideoIdOrderByLikeCountDesc(
                    testVideo.getId()
            );

            // then
            assertThat(found).hasSize(3);
            assertThat(found.get(0).getLikeCount()).isEqualTo(500);
            assertThat(found.get(1).getLikeCount()).isEqualTo(200);
            assertThat(found.get(2).getLikeCount()).isEqualTo(100);
        }

        @Test
        @DisplayName("좋아요 수가 같으면 순서가 보장되지 않는다")
        void orderByLikeCount_SameCount_AnyOrder() {
            // given
            Comment comment1 = createComment(testVideo, "api-1", "댓글1");
            comment1.setLikeCount(100);

            Comment comment2 = createComment(testVideo, "api-2", "댓글2");
            comment2.setLikeCount(100);

            commentRepository.saveAll(List.of(comment1, comment2));

            // when
            List<Comment> found = commentRepository.findByVideoIdOrderByLikeCountDesc(
                    testVideo.getId()
            );

            // then
            assertThat(found).hasSize(2);
            assertThat(found).allMatch(c -> c.getLikeCount() == 100);
        }
    }

    @Nested
    @DisplayName("findTop5ByVideoIdOrderByLikeCountDesc 메서드는")
    class FindTop5ByVideoIdTest {

        @Test
        @DisplayName("좋아요 순 상위 5개만 조회한다")
        void findTop5_WithMore_ReturnsTop5() {
            // given
            for (int i = 1; i <= 10; i++) {
                Comment comment = createComment(testVideo, "api-" + i, "댓글" + i);
                comment.setLikeCount(i * 10);
                commentRepository.save(comment);
            }

            // when
            List<Comment> found = commentRepository.findTop5ByVideoIdOrderByLikeCountDesc(
                    testVideo.getId()
            );

            // then
            assertThat(found).hasSize(5);
            assertThat(found.get(0).getLikeCount()).isEqualTo(100);
            assertThat(found.get(4).getLikeCount()).isEqualTo(60);
        }

        @Test
        @DisplayName("댓글이 5개 미만이면 있는 만큼만 반환한다")
        void findTop5_LessThan5_ReturnsAll() {
            // given
            for (int i = 1; i <= 3; i++) {
                Comment comment = createComment(testVideo, "api-" + i, "댓글" + i);
                comment.setLikeCount(i * 10);
                commentRepository.save(comment);
            }

            // when
            List<Comment> found = commentRepository.findTop5ByVideoIdOrderByLikeCountDesc(
                    testVideo.getId()
            );

            // then
            assertThat(found).hasSize(3);
        }

        @Test
        @DisplayName("댓글이 없으면 빈 리스트를 반환한다")
        void findTop5_NoComments_ReturnsEmpty() {
            // when
            List<Comment> found = commentRepository.findTop5ByVideoIdOrderByLikeCountDesc(
                    testVideo.getId()
            );

            // then
            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("deleteByVideoId 메서드는")
    class DeleteByVideoIdTest {

        @Test
        @DisplayName("특정 비디오의 모든 댓글을 삭제한다")
        void deleteByVideoId_WithComments_DeletesAll() {
            // given
            Comment comment1 = createComment(testVideo, "api-1", "댓글1");
            Comment comment2 = createComment(testVideo, "api-2", "댓글2");
            commentRepository.saveAll(List.of(comment1, comment2));

            // when
            commentRepository.deleteByVideoId(testVideo.getId());
            commentRepository.flush();

            // then
            List<Comment> found = commentRepository.findByVideoId(testVideo.getId());
            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("다른 비디오의 댓글은 삭제되지 않는다")
        void deleteByVideoId_OtherVideo_NotDeleted() {
            // given
            Video otherVideo = createAndSaveVideo("other-video", "다른 영상");
            Comment myComment = createComment(testVideo, "api-1", "내 댓글");
            Comment otherComment = createComment(otherVideo, "api-2", "다른 댓글");
            commentRepository.saveAll(List.of(myComment, otherComment));

            // when
            commentRepository.deleteByVideoId(testVideo.getId());
            commentRepository.flush();

            // then
            List<Comment> remainingComments = commentRepository.findByVideoId(otherVideo.getId());
            assertThat(remainingComments).hasSize(1);
            assertThat(remainingComments.get(0).getApiCommentId()).isEqualTo("api-2");
        }
    }

    // ========== Helper Methods ==========

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

    private Comment createComment(Video video, String apiCommentId, String content) {
        return createComment(video, apiCommentId, content, SentimentType.OTHER);
    }

    private Comment createComment(Video video, String apiCommentId, String content, SentimentType sentimentType) {
        return Comment.builder()
                .video(video)
                .apiCommentId(apiCommentId)
                .commentContent(content)
                .likeCount(0)
                .sentimentType(sentimentType)
                .detailSentiments(List.of(DetailSentimentType.NEUTRAL))
                .writer("테스트 작성자")
                .writtenAt("2024-01-01T00:00:00Z")
                .hasReplies(false)
                .build();
    }
}