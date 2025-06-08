package com.knu.sosuso.capstone.service;

import com.knu.sosuso.capstone.domain.Comment;
import com.knu.sosuso.capstone.domain.Video;
import com.knu.sosuso.capstone.domain.value.SentimentType;
import com.knu.sosuso.capstone.dto.response.comment.CommentDto;
import com.knu.sosuso.capstone.dto.response.comment.CommentResponse;
import com.knu.sosuso.capstone.repository.CommentRepository;
import com.knu.sosuso.capstone.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentQueryService {

    private final CommentRepository commentRepository;
    private final VideoRepository videoRepository;

    /**
     * 단일 조건 댓글 검색
     */
    public CommentResponse searchComments(String apiVideoId, String q, String keyword, String sentiment) {
        // 비디오 존재 확인
        Video video = videoRepository.findByApiVideoId(apiVideoId)
                .orElseThrow(() -> new IllegalArgumentException("비디오를 찾을 수 없습니다: " + apiVideoId));

        List<Comment> comments;

        // 단일 조건에 따라 검색
        if (q != null) {
            // 일반 텍스트 검색
            comments = commentRepository.findByVideoIdAndTextContaining(video.getId(), q.trim());

        } else if (keyword != null) {
            // AI 키워드 검색
            comments = commentRepository.findByVideoIdAndTextContaining(video.getId(), keyword.trim());

        } else if (sentiment != null) {
            // 감정별 검색
            SentimentType sentimentType = parseSentimentType(sentiment);
            comments = commentRepository.findByVideoIdAndSentimentTypeOrderById(video.getId(), sentimentType);

        } else {
            throw new IllegalArgumentException("검색 조건이 필요합니다.");
        }

        // DTO 변환
        List<CommentDto> commentDtos = comments.stream()
                .map(comment -> new CommentDto(
                        comment.getApiCommentId(),
                        comment.getWriter(),
                        comment.getCommentContent(),
                        comment.getLikeCount(),
                        comment.getSentimentType() != null ?
                                comment.getSentimentType().name().toLowerCase() : null,
                        comment.getWrittenAt()
                ))
                .toList();

        return new CommentResponse(apiVideoId, commentDtos);
    }

    private SentimentType parseSentimentType(String sentiment) {
        try {
            return SentimentType.valueOf(sentiment.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("유효하지 않은 감정 타입입니다: " + sentiment);
        }
    }
}