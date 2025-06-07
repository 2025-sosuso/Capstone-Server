package com.knu.sosuso.capstone.dto.response.search;

public record UrlVideoDto(
        String id,
        String title,
        String description,
        String publishedAt,
        String thumbnailUrl,
        Long viewCount,
        Long likeCount,
        Integer commentCount,
        Boolean isScrapped,
        Long scrapId
) {
}
