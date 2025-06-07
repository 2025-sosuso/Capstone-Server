package com.knu.sosuso.capstone.repository;

import com.knu.sosuso.capstone.domain.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    // 특정 비디오의 댓글들 조회
    List<Comment> findByVideoIdOrderByIdAsc(Long videoId);

    // 중복 댓글 체크
    boolean existsByApiCommentId(String apiCommentId);

    // 비디오별 댓글 삭제
    void deleteByVideoId(Long videoId);

    List<Comment> findAllByVideoId(Long videoId);

    // 특정 비디오에 댓글이 있는지 확인
    boolean existsByVideoId(Long videoId);

    // 특정 비디오의 댓글 개수 조회
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.video.id = :videoId")
    Long countByVideoId(@Param("videoId") Long videoId);

    // 특정 비디오의 댓글에서 키워드 검색 (관련도순 - 기존 순서 유지)
    @Query("SELECT c FROM Comment c WHERE c.video.id = :videoId AND c.commentContent LIKE %:keyword% ORDER BY c.id ASC")
    List<Comment> findByVideoIdAndCommentContentContaining(
            @Param("videoId") Long videoId,
            @Param("keyword") String keyword);

}
