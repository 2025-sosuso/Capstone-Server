package com.knu.sosuso.capstone.domain.comment.service;

import com.knu.sosuso.capstone.domain.ai.dto.AIAnalysisResponse;
import com.knu.sosuso.capstone.domain.comment.dto.CommentDto;
import com.knu.sosuso.capstone.domain.comment.dto.response.CommentApiResponse;
import com.knu.sosuso.capstone.domain.comment.entity.Comment;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Comment 엔티티와 DTO 간의 변환을 담당하는 Mapper
 * 중복된 변환 로직을 제거하고 일관성 있는 변환 제공
 */
@Component
public class CommentMapper {

    /**
     * Comment 엔티티 → CommentDto 변환 (DB에서 조회한 경우)
     */
    public CommentDto toDto(Comment comment) {
        return new CommentDto(
                comment.getApiCommentId(),
                comment.getWriter(),
                comment.getCommentContent(),
                comment.getLikeCount(),
                comment.getSentimentType() != null ?
                        comment.getSentimentType().name().toUpperCase() : null,
                comment.getWrittenAt(),
                comment.getHasReplies() != null && comment.getHasReplies()
        );
    }

    /**
     * Comment 엔티티 리스트 → CommentDto 리스트 변환
     */
    public List<CommentDto> toDtoList(List<Comment> comments) {
        return comments.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * CommentApiResponse.CommentData → CommentDto 변환 (YouTube API에서 가져온 경우)
     */
    public CommentDto toDto(CommentApiResponse.CommentData commentData) {
        return new CommentDto(
                commentData.id(),
                commentData.authorName(),
                commentData.commentText(),
                commentData.likeCount(),
                null,  // sentiment는 AI 분석 전이므로 null
                commentData.publishedAt(),
                commentData.hasReplies()
        );
    }

    /**
     * CommentApiResponse.CommentData + AI 분석 결과 → CommentDto 변환
     */
    public CommentDto toDtoWithAI(
            CommentApiResponse.CommentData commentData,
            AIAnalysisResponse analysisResponse) {

        String sentiment = null;
        if (analysisResponse != null &&
                analysisResponse.sentimentComments().containsKey(commentData.id())) {
            sentiment = analysisResponse.sentimentComments()
                    .get(commentData.id()).name().toUpperCase();
        }

        return new CommentDto(
                commentData.id(),
                commentData.authorName(),
                commentData.commentText(),
                commentData.likeCount(),
                sentiment,
                commentData.publishedAt(),
                commentData.hasReplies()
        );
    }

    /**
     * CommentApiResponse.CommentData 리스트 → CommentDto 리스트 변환
     * AI 분석 결과가 있는 경우 sentiment 포함
     */
    public List<CommentDto> toDtoList(
            List<CommentApiResponse.CommentData> commentDataList,
            AIAnalysisResponse analysisResponse) {

        return commentDataList.stream()
                .map(commentData -> {
                    if (analysisResponse != null) {
                        return toDtoWithAI(commentData, analysisResponse);
                    } else {
                        return toDto(commentData);
                    }
                })
                .collect(Collectors.toList());
    }
}