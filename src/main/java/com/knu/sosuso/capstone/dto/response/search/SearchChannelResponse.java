package com.knu.sosuso.capstone.dto.response.search;

import java.util.List;

public record SearchChannelResponse(
        List<ChannelSearchResult> results
) {
    public record ChannelSearchResult(
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
