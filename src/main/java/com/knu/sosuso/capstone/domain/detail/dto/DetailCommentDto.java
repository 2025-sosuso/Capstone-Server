package com.knu.sosuso.capstone.domain.detail.dto;

public record DetailCommentDto(
        String id,
        String author,
        String text,
        Integer likeCount,
        String sentiment,
        String publishedAt
) {
}
