package com.knu.sosuso.capstone.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record VideoApiResponse(
        String id,
        String publishedAt,
        String title,
        String description,
        String channelId,
        String channelTitle,
        String channelThumbnailUrl,
        String subscriberCount,
        String thumbnailUrl,
        String[] tags,
        String categoryId,
        String liveBroadcastContent,
        String defaultAudioLanguage,
        String viewCount,
        String likeCount,
        String commentCount

) {
}