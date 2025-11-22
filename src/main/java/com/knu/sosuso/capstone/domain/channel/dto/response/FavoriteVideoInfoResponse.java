package com.knu.sosuso.capstone.domain.channel.dto.response;

import com.knu.sosuso.capstone.domain.comment.dto.CommentDto;
import com.knu.sosuso.capstone.domain.common.dto.SentimentDistribution;

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
            SentimentDistribution sentimentDistribution,
            List<String> keywords,
            List<CommentDto> topComments
    ) {}
}
