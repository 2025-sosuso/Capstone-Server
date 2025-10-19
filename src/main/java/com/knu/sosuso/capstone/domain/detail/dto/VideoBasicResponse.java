package com.knu.sosuso.capstone.domain.detail.dto;

public record VideoBasicResponse(
        DetailVideoDto video,
        DetailChannelDto channel
) {
}