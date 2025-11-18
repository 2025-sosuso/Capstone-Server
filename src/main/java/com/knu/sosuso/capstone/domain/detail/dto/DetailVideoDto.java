package com.knu.sosuso.capstone.domain.detail.dto;

public record DetailVideoDto(
        String id,
        String title,
        String description,
        String publishedAt,
        String thumbnailUrl,
        Long viewCount,
        Long likeCount,
        Integer commentCount
) {
}
