package com.knu.sosuso.capstone.dto.response;

import com.knu.sosuso.capstone.dto.response.favorite_channel.FavoriteChannelListResponse;
import com.knu.sosuso.capstone.dto.response.favorite_channel.FavoriteVideoInfoResponse;

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