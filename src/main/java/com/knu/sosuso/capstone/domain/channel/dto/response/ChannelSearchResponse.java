package com.knu.sosuso.capstone.domain.channel.dto.response;

import java.util.List;

public record ChannelSearchResponse(
        List<ChannelDto> results
) {
    public record ChannelDto(
            String id,
            String title,
            String handle,
            String description,
            String thumbnailUrl,
            Long subscriberCount,
            Long favoriteChannelId
    ) {
    }
}
