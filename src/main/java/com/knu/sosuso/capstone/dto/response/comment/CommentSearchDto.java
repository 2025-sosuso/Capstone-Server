package com.knu.sosuso.capstone.dto.response.comment;

public record CommentSearchDto(
        String id,
        String author,
        String text,
        Integer likeCount,
        String sentiment,
        String publishedAt
) {
}
