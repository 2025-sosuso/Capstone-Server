package com.knu.sosuso.capstone.domain.comment.dto;

public record ReplyDto(
        String id,
        String author,
        String text,
        Integer likeCount,
        String publishedAt
) {}