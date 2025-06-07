package com.knu.sosuso.capstone.dto.response.search;

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
            Boolean isFavorited
    ) {
    }
}
