package com.knu.sosuso.capstone.service;

import com.knu.sosuso.capstone.domain.Comment;
import com.knu.sosuso.capstone.dto.response.comment.CommentSearchDto;
import com.knu.sosuso.capstone.dto.response.comment.CommentSearchResponse;
import com.knu.sosuso.capstone.repository.CommentRepository;
import com.knu.sosuso.capstone.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentQueryService {

    private final CommentRepository commentRepository;
    private final VideoRepository videoRepository;

    /**
     * 특정 비디오의 댓글 검색 (관련도순)
     */
    public CommentSearchResponse searchComments(Long videoId, String query) {
        // 입력값 검증
        validateVideoAndQuery(videoId, query);

        // 비디오 존재 여부 확인
        if (!videoRepository.existsById(videoId)) {
            throw new IllegalArgumentException("존재하지 않는 비디오입니다: " + videoId);
        }

        String keyword = query.trim();
        log.info("댓글 검색 시작: videoId={}, keyword={}", videoId, keyword);

        // 키워드로 댓글 검색 (관련도순 - 기존 순서 유지)
        List<Comment> matchedComments = commentRepository
                .findByVideoIdAndCommentContentContaining(videoId, keyword);

        // 응답 변환
        List<CommentSearchDto> results = matchedComments.stream()
                .map(this::mapToCommentSearchDto)
                .collect(Collectors.toList());

        log.info("댓글 검색 완료: 반환된 댓글 수={}", results.size());

        return new CommentSearchResponse(videoId, results);
    }

    /**
     * Comment를 CommentSearchDto로 변환
     */
    private CommentSearchDto mapToCommentSearchDto(Comment comment) {
        return new CommentSearchDto(
                comment.getApiCommentId(),
                comment.getWriter(),
                comment.getCommentContent(),
                comment.getLikeCount(),
                comment.getSentimentType() != null ? comment.getSentimentType().name().toUpperCase() : null,
                comment.getWrittenAt()
        );
    }


    /**
     * 비디오 ID와 검색어 검증
     */
    private void validateVideoAndQuery(Long videoId, String query) {
        if (videoId == null || videoId <= 0) {
            throw new IllegalArgumentException("유효하지 않은 비디오 ID입니다");
        }

        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("검색어는 필수입니다");
        }
    }

    // TODO: 나중에 추가할 기능들
    // public CommentFilterResponse getCommentsBySentiment(Long videoId, SentimentType sentiment)
    // public CommentFilterResponse getCommentsByKeyword(Long videoId, String keyword)
}