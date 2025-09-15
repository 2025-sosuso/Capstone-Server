package com.knu.sosuso.capstone.domain.channel.dto.request;

public record RegisterFavoriteChannelRequest(
        String apiChannelId,
        String apiChannelName,
        String apiChannelThumbnail
) {
}
