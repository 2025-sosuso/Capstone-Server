package com.knu.sosuso.capstone.dto.response.favorite_channel;

public record FavoriteChannelListResponse(
        Long favoriteChannelId,
        String apiChannelId,
        String apiChannelName,
        String apiChannelThumbnail
) {
}
