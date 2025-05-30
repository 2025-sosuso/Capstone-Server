package com.knu.sosuso.capstone.repository;

import com.knu.sosuso.capstone.domain.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    // 특정 비디오의 댓글들 조회 (좋아요 수 내림차순)
    List<Comment> findByVideoIdOrderByLikeCountDesc(String videoId);

    // 중복 댓글 체크
    boolean existsByCommentId(String apiCommentId);

    // 비디오별 댓글 삭제
    void deleteByVideoId(String videoId);

}
