package com.knu.sosuso.capstone.global.service.mapper;

import com.knu.sosuso.capstone.domain.comment.dto.CommentDto;
import com.knu.sosuso.capstone.domain.comment.entity.Comment;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
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
                comment.getHasReplies() != null && comment.getHasReplies(),
                comment.getDetailSentiments() != null ?
                        comment.getDetailSentiments().stream()
                                .map(Enum::name)
                                .collect(Collectors.toList()) :
                        new ArrayList<>()
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
     * Comment → CommentDto 변환 (TOP 5 댓글용)
     * hasReplies를 강제로 false로 설정
     */
    public CommentDto toTopCommentDto(Comment comment) {
        return new CommentDto(
                comment.getApiCommentId(),
                comment.getWriter(),
                comment.getCommentContent(),
                comment.getLikeCount(),
                comment.getSentimentType() != null ?
                        comment.getSentimentType().name().toUpperCase() : null,
                comment.getWrittenAt(),
                false,  // TOP 5 댓글은 항상 false
                comment.getDetailSentiments() != null ?
                        comment.getDetailSentiments().stream()
                                .map(Enum::name)
                                .collect(Collectors.toList()) :
                        new ArrayList<>()
        );
    }

    /**
     * DB에서 가져온 TOP 5 댓글 리스트 변환
     */
    public List<CommentDto> toTopCommentDtoList(List<Comment> comments) {
        return comments.stream()
                .map(this::toTopCommentDto)
                .collect(Collectors.toList());
    }
}