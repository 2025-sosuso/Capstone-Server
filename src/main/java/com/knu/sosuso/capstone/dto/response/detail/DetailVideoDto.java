package com.knu.sosuso.capstone.dto.response.detail;

public record DetailVideoDto(
        String id,
        String title,
        String description,
        String publishedAt,
        String thumbnailUrl,
        Long viewCount,
        Long likeCount,
        Integer commentCount,
        Long scrapId
) {
}
