package com.knu.sosuso.capstone.dto.response.favorite_channel;

public record FavoriteChannelListResponse(
        Long favoriteChannelId,
        String apiChannelName,
        String apiChannelThumbnail
) {
}
