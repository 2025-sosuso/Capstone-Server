package com.knu.sosuso.capstone.dto.response.detail;

public record DetailChannelDto(
        String id,
        String title,
        String thumbnailUrl,
        Long subscriberCount,
        Long favoriteChannelId
) {
}
