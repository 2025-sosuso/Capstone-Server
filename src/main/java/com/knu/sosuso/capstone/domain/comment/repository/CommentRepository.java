package com.knu.sosuso.capstone.domain.comment.repository;

import com.knu.sosuso.capstone.domain.comment.entity.Comment;
import com.knu.sosuso.capstone.domain.comment.entity.value.SentimentType;
import com.knu.sosuso.capstone.domain.video.entity.Video;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    // 특정 비디오의 댓글들 조회 (Video 엔티티로)
    List<Comment> findByVideo(Video video);

    // 특정 비디오의 모든 댓글 조회 (DB ID로, 정렬 없음)
    List<Comment> findByVideoId(Long videoId);

    // 중복 댓글 체크
    boolean existsByApiCommentId(String apiCommentId);

    // 비디오별 댓글 삭제 (DB ID로)
    void deleteByVideoId(Long videoId);

    // 일반 텍스트 검색
    @Query("SELECT c FROM Comment c WHERE c.video.id = :videoId " +
            "AND LOWER(c.commentContent) LIKE LOWER(CONCAT('%', :searchText, '%')) " +
            "ORDER BY c.id ASC")
    List<Comment> findByVideoIdAndTextContaining(@Param("videoId") Long videoId,
                                                 @Param("searchText") String searchText);

    // 감정별 조회
    List<Comment> findByVideoIdAndSentimentTypeOrderById(Long videoId, SentimentType sentimentType);

    // 좋아요 수 기준 정렬
    List<Comment> findByVideoIdOrderByLikeCountDesc(Long videoId);

    // 좋아요 순 상위 5개 댓글 조회
    List<Comment> findTop5ByVideoIdOrderByLikeCountDesc(Long videoId);


    // API 댓글 ID로 조회
    Optional<Comment> findByApiCommentId(String apiCommentId);
}