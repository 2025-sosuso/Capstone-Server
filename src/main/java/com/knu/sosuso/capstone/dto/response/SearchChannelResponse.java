package com.knu.sosuso.capstone.dto.response;

import java.util.List;

public record SearchChannelResponse(
        List<ChannelData> channels
) {
    public record ChannelData(
            String channelId,
            String title,
            String handle,
            String description,
            String thumbnailUrl,
            String subscriberCount,
            boolean isSubscribed
    ) {
    }
}
