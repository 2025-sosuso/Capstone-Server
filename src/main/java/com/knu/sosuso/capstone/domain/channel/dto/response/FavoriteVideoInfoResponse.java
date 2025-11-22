package com.knu.sosuso.capstone.domain.channel.dto.response;

import com.knu.sosuso.capstone.domain.comment.dto.CommentDto;
import com.knu.sosuso.capstone.domain.common.dto.ChannelBasicDto;
import com.knu.sosuso.capstone.domain.common.dto.SentimentDistribution;
import com.knu.sosuso.capstone.domain.common.dto.VideoBasicDto;

import java.util.List;

public record FavoriteVideoInfoResponse (
        VideoBasicDto video,
        ChannelBasicDto channel,
        Analysis analysis

){

    public record Analysis(
            String summary,
            SentimentDistribution sentimentDistribution,
            List<String> keywords,
            List<CommentDto> topComments
    ) {}
}
