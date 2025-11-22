package com.knu.sosuso.capstone.domain.main;

import com.knu.sosuso.capstone.domain.channel.dto.response.FavoriteChannelListResponse;
import com.knu.sosuso.capstone.domain.channel.dto.response.FavoriteVideoInfoResponse;

import java.util.List;

/**
 * 메인 페이지 - 관심 채널 섹션 응답
 */
public record FavoriteChannelSectionResponse(
        List<FavoriteChannelListResponse> favoriteChannelList,
        FavoriteVideoInfoResponse videoSummary
) {
}