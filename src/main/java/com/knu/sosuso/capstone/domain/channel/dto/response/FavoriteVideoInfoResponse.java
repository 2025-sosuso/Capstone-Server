package com.knu.sosuso.capstone.domain.channel.dto.response;

import com.knu.sosuso.capstone.domain.detail.dto.DetailCommentDto;

import java.util.List;

public record FavoriteVideoInfoResponse (
        Video video,
        Channel channel,
        Analysis analysis

){
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
            FavoriteVideoInfoResponse.SentimentDistribution sentimentDistribution,
            List<String> keywords,
            List<DetailCommentDto> topComments
    ) {}

    public record SentimentDistribution(
            Double positive,
            Double negative,
            Double other
    ) {}
}
