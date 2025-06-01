package com.knu.sosuso.capstone.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record VideoApiResponse(
        String apiVideoId,
        String title,
        String description,
        String thumbnailUrl,
        String channelId,
        String channelTitle,
        String channelThumbnailUrl,
        String publishedAt,
        String subscriberCount,
        String viewCount,
        String likeCount,
        String commentCount
) {
}