package com.knu.sosuso.capstone.dto;

import java.util.List;

public record ChannelApiResponse(
        List<ChannelData> channels
) {
    public record ChannelData(
            String channelId,
            String title,
            String description,
            String thumbnailUrl,
            String subscriberCount  // 구독자 수
    ) {
    }
}
