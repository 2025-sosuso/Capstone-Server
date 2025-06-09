package com.knu.sosuso.capstone.dto.response.detail;

public record DetailCommentDto(
        String id,
        String author,
        String text,
        Integer likeCount,
        String sentiment,
        String publishedAt
) {
}
