package com.knu.sosuso.capstone.dto.response.search;

public record UrlCommentDto(
        String id,
        String author,
        String text,
        Integer likeCount,
        String sentiment,
        String publishedAt
) {
}
