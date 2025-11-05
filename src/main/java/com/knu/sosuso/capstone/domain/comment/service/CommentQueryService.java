package com.knu.sosuso.capstone.domain.comment.service;

import com.knu.sosuso.capstone.domain.comment.entity.Comment;
import com.knu.sosuso.capstone.domain.comment.dto.CommentDto;
import com.knu.sosuso.capstone.domain.video.entity.Video;
import com.knu.sosuso.capstone.domain.comment.entity.value.SentimentType;
import com.knu.sosuso.capstone.domain.comment.dto.response.CommentResponse;
import com.knu.sosuso.capstone.domain.comment.repository.CommentRepository;
import com.knu.sosuso.capstone.domain.video.repository.VideoRepository;
import com.knu.sosuso.capstone.global.exception.BusinessException;
import com.knu.sosuso.capstone.global.exception.error.CommentError;
import com.knu.sosuso.capstone.global.exception.error.VideoError;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentQueryService {

    private final CommentMapper commentMapper;
    private final CommentRepository commentRepository;
    private final VideoRepository videoRepository;

    /**
     * 단일 조건 댓글 검색
     */
    public CommentResponse searchComments(String apiVideoId, String q, String keyword, String sentiment) {
        // 비디오 존재 확인
        Video video = videoRepository.findByApiVideoId(apiVideoId)
                .orElseThrow(() -> new BusinessException(VideoError.VIDEO_NOT_FOUND));

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
            throw new BusinessException(CommentError.COMMENT_SEARCH_CONDITION_REQUIRED);
        }

        // DTO 변환
        List<CommentDto> commentDtos = commentMapper.toDtoList(comments);

        return new CommentResponse(apiVideoId, commentDtos);
    }

    private SentimentType parseSentimentType(String sentiment) {
        try {
            return SentimentType.valueOf(sentiment.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(CommentError.INVALID_SENTIMENT_TYPE);
        }
    }
}