package com.knu.sosuso.capstone.domain.detail.dto;

public record DetailChannelDto(
        String id,
        String title,
        String thumbnailUrl,
        Long subscriberCount
) {
}
