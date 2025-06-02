package com.knu.sosuso.capstone.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record VideoApiResponse(
        String apiVideoId,
        String title,
        String description,
        String viewCount,
        String likeCount,
        String commentCount,
        String thumbnailUrl,
        String channelId,
        String channelTitle,
        String channelThumbnailUrl,
        String subscriberCount,
        String publishedAt
) {
}