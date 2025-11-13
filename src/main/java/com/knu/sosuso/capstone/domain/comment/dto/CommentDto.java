package com.knu.sosuso.capstone.domain.comment.dto;

import java.util.List;

public record CommentDto(
        String id,
        String author,
        String text,
        Integer likeCount,
        String sentiment,
        String publishedAt,
        boolean hasReplies,
        List<String> detailSentiments
) {
}
