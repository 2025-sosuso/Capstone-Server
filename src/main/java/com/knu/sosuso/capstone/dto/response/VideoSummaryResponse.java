package com.knu.sosuso.capstone.dto.response;

import java.util.List;

public record VideoSummaryResponse(
        String apiVideoId,
        String title,
        String thumbnailUrl,
        String channelName,
        Long subscriberCount,
        Long viewCount,
        Long likeCount,
        Integer commentCount,
        String uploadedAt,
        String summary,
        SentimentDistribution sentimentDistribution,
        List<String> keywords
) {
    public record SentimentDistribution(
            Double positive,
            Double negative,
            Double other
    ) {}
}
