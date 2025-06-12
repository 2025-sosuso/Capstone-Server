package com.knu.sosuso.capstone.dto.response;

import java.util.List;

public record VideoSummaryResponse(
        Video video,
        Channel channel,
        Analysis analysis
) {
    public record Video(
            String id,
            String title,
            String description,
            String publishedAt,
            String thumbnailUrl,
            Long viewCount,
            Long likeCount,
            Integer commentCount
    ) {}

    public record Channel(
            String id,
            String title,
            String thumbnailUrl,
            Long subscriberCount
    ) {}

    public record Analysis(
            String summary,
            SentimentDistribution sentimentDistribution,
            List<String> keywords
    ) {}

    public record SentimentDistribution(
            Double positive,
            Double negative,
            Double other
    ) {}
}