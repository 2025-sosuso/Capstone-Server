package com.knu.sosuso.capstone.service;

import com.knu.sosuso.capstone.domain.Comment;
import com.knu.sosuso.capstone.domain.value.SentimentType;
import com.knu.sosuso.capstone.dto.response.comment.CommentDto;
import com.knu.sosuso.capstone.dto.response.comment.CommentResponse;
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
     * 댓글 조회 (감정 필터링, 키워드 검색, 복합 조건)
     */
    public CommentResponse getComments(String apiVideoId, String sentiment, String q) {
        // 입력값 검증
        validateApiVideoId(apiVideoId);
        validateVideoExists(apiVideoId);

        log.info("댓글 조회 시작: apiVideoId={}, sentiment={}, q={}", apiVideoId, sentiment, q);

        List<Comment> comments;

        if (sentiment != null && q != null) {
            // 복합 조건: 감정 + 키워드
            comments = getCommentsBySentimentAndKeyword(apiVideoId, sentiment, q);
        } else if (sentiment != null) {
            // 감정 필터링만
            comments = getCommentsBySentimentOnly(apiVideoId, sentiment);
        } else {
            // 키워드 검색만
            comments = getCommentsByKeywordOnly(apiVideoId, q);
        }

        // 응답 변환
        List<CommentDto> results = comments.stream()
                .map(this::mapToCommentDto)
                .collect(Collectors.toList());

        log.info("댓글 조회 완료: 반환된 댓글 수={}", results.size());

        return new CommentResponse(apiVideoId, results);
    }

    /**
     * 감정 + 키워드 복합 조건 검색
     */
    private List<Comment> getCommentsBySentimentAndKeyword(String apiVideoId, String sentiment, String keyword) {
        SentimentType sentimentType = parseSentimentType(sentiment);
        String trimmedKeyword = keyword.trim();

        log.info("복합 조건 검색: sentiment={}, keyword={}", sentimentType.name(), trimmedKeyword);

        return commentRepository.findByApiVideoIdAndSentimentTypeAndCommentContentContaining(
                apiVideoId, sentimentType, trimmedKeyword);
    }

    /**
     * 감정 필터링만
     */
    private List<Comment> getCommentsBySentimentOnly(String apiVideoId, String sentiment) {
        SentimentType sentimentType = parseSentimentType(sentiment);

        log.info("감정 필터링: sentiment={}", sentimentType.name());

        return commentRepository.findByApiVideoIdAndSentimentTypeOrderByIdAsc(apiVideoId, sentimentType);
    }

    /**
     * 키워드 검색만
     */
    private List<Comment> getCommentsByKeywordOnly(String apiVideoId, String keyword) {
        String trimmedKeyword = keyword.trim();

        if (trimmedKeyword.isEmpty()) {
            throw new IllegalArgumentException("검색어는 필수입니다");
        }

        log.info("키워드 검색: keyword={}", trimmedKeyword);

        return commentRepository.findByApiVideoIdAndCommentContentContaining(apiVideoId, trimmedKeyword);
    }

    /**
     * 비디오 존재 여부 확인
     */
    private void validateVideoExists(String apiVideoId) {
        if (!videoRepository.findByApiVideoId(apiVideoId).isPresent()) {
            throw new IllegalArgumentException("존재하지 않는 비디오입니다: " + apiVideoId);
        }
    }

    /**
     * 감정 문자열을 SentimentType으로 변환
     */
    private SentimentType parseSentimentType(String sentiment) {
        try {
            return SentimentType.valueOf(sentiment.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("지원하지 않는 감정 타입입니다: " + sentiment +
                    ". 지원 타입: POSITIVE, NEGATIVE, OTHER");
        }
    }

    /**
     * Comment를 CommentDto로 변환
     */
    private CommentDto mapToCommentDto(Comment comment) {
        return new CommentDto(
                comment.getApiCommentId(),
                comment.getWriter(),
                comment.getCommentContent(),
                comment.getLikeCount(),
                comment.getSentimentType() != null ? comment.getSentimentType().name() : null,
                comment.getWrittenAt()
        );
    }

    /**
     * API 비디오 ID 검증
     */
    private void validateApiVideoId(String apiVideoId) {
        if (apiVideoId == null || apiVideoId.trim().isEmpty()) {
            throw new IllegalArgumentException("유효하지 않은 비디오 ID입니다");
        }
    }
}