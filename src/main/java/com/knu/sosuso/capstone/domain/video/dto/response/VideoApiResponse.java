package com.knu.sosuso.capstone.domain.video.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

@JsonIgnoreProperties(ignoreUnknown = true)
public record VideoApiResponse(
        String apiVideoId,
        String title,
        String description,
        String viewCount,
        String likeCount,
        String commentCount,
        String thumbnailUrl,
        JsonNode thumbnails,
        String channelId,
        String channelTitle,
        String channelThumbnailUrl,
        String subscriberCount,
        String publishedAt
) {
}