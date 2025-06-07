package com.knu.sosuso.capstone.dto.response.search;

public record UrlChannelDto(
        String id,
        String title,
        String thumbnailUrl,
        Long subscriberCount,
        Boolean isFavorited
) {
}
