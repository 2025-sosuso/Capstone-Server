package com.knu.sosuso.capstone.domain.channel.dto.response;

public record FavoriteChannelListResponse(
        Long favoriteChannelId,
        String apiChannelId,
        String apiChannelName,
        String apiChannelThumbnail
) {
}
