package com.knu.sosuso.capstone.domain.main;

import com.knu.sosuso.capstone.domain.channel.dto.response.FavoriteChannelListResponse;
import com.knu.sosuso.capstone.domain.channel.dto.response.FavoriteVideoInfoResponse;
import com.knu.sosuso.capstone.domain.video.dto.response.VideoSummaryResponse;

import java.util.List;

public record MainPageResponse(
        FavoriteChannelResponse favoriteChannelVideo,
        List<VideoSummaryResponse> trendingVideos,
        List<VideoSummaryResponse> scrapVideos
) {

    public record FavoriteChannelResponse(
            List<FavoriteChannelListResponse> favoriteChannelList,
            FavoriteVideoInfoResponse videoSummary
    ) {}
}