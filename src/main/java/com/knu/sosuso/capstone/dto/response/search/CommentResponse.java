package com.knu.sosuso.capstone.dto.response.search;

public record CommentResponse(  // URL 검색 - 댓글 정보 DTO
        String id,
        String author,
        String text,
        Integer likeCount,
        String sentiment,
        String publishedAt
) {
}
