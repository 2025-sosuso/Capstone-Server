package com.knu.sosuso.capstone.domain.comment.dto;

public record CommentDto(
        String id,
        String author,
        String text,
        Integer likeCount,
        String sentiment,
        String publishedAt
) {
}
