package com.knu.sosuso.capstone.dto.request;

public record RegisterFavoriteChannelRequest(
        String apiChannelId,
        String apiChannelName,
        String apiChannelThumbnail
) {
}
