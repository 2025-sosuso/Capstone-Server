package com.knu.sosuso.capstone.repository;

import com.knu.sosuso.capstone.domain.Comment;
import com.knu.sosuso.capstone.domain.value.SentimentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    // 특정 비디오의 댓글들 조회 (DB ID로)
    List<Comment> findByVideoIdOrderByIdAsc(Long videoId);

    // 중복 댓글 체크
    boolean existsByApiCommentId(String apiCommentId);

    // 비디오별 댓글 삭제 (DB ID로)
    void deleteByVideoId(Long videoId);

    // 모든 댓글 조회 (DB ID로)
    List<Comment> findAllByVideoId(Long videoId);

    // 특정 비디오에 댓글이 있는지 확인 (DB ID로)
    boolean existsByVideoId(Long videoId);

    // 특정 비디오의 댓글 개수 조회 (DB ID로)
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.video.id = :videoId")
    Long countByVideoId(@Param("videoId") Long videoId);

    // === API ID 기반 조회 메서드들 (프론트엔드용) ===

    // 감정별 댓글 조회 (apiVideoId 기반)
    @Query("SELECT c FROM Comment c WHERE c.video.apiVideoId = :apiVideoId AND c.sentimentType = :sentimentType ORDER BY c.id ASC")
    List<Comment> findByApiVideoIdAndSentimentTypeOrderByIdAsc(
            @Param("apiVideoId") String apiVideoId,
            @Param("sentimentType") SentimentType sentimentType);

    // 특정 비디오의 댓글에서 키워드 검색 (apiVideoId 기반)
    @Query("SELECT c FROM Comment c WHERE c.video.apiVideoId = :apiVideoId AND c.commentContent LIKE %:keyword% ORDER BY c.id ASC")
    List<Comment> findByApiVideoIdAndCommentContentContaining(
            @Param("apiVideoId") String apiVideoId,
            @Param("keyword") String keyword);

    // 감정 + 키워드 복합 조건 검색
    @Query("SELECT c FROM Comment c WHERE c.video.apiVideoId = :apiVideoId AND c.sentimentType = :sentimentType AND c.commentContent LIKE %:keyword% ORDER BY c.id ASC")
    List<Comment> findByApiVideoIdAndSentimentTypeAndCommentContentContaining(
            @Param("apiVideoId") String apiVideoId,
            @Param("sentimentType") SentimentType sentimentType,
            @Param("keyword") String keyword);

    // 특정 API 비디오의 모든 댓글 조회
    @Query("SELECT c FROM Comment c WHERE c.video.apiVideoId = :apiVideoId ORDER BY c.id ASC")
    List<Comment> findAllByApiVideoId(@Param("apiVideoId") String apiVideoId);

    // API 비디오에 댓글이 있는지 확인
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Comment c WHERE c.video.apiVideoId = :apiVideoId")
    boolean existsByApiVideoId(@Param("apiVideoId") String apiVideoId);

    // API 비디오의 댓글 개수 조회
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.video.apiVideoId = :apiVideoId")
    Long countByApiVideoId(@Param("apiVideoId") String apiVideoId);

}
