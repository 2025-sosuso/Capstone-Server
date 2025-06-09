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

    // 일반 텍스트 검색
    @Query("SELECT c FROM Comment c WHERE c.video.id = :videoId " +
            "AND LOWER(c.commentContent) LIKE LOWER(CONCAT('%', :searchText, '%')) " +
            "ORDER BY c.id ASC")
    List<Comment> findByVideoIdAndTextContaining(@Param("videoId") Long videoId,
                                                 @Param("searchText") String searchText);

    // 감정별 조회
    List<Comment> findByVideoIdAndSentimentTypeOrderById(Long videoId, SentimentType sentimentType);
}

