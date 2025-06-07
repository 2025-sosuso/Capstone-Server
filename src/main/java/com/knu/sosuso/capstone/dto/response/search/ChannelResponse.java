package com.knu.sosuso.capstone.dto.response.search;

public record ChannelResponse(  // URL 검색 - 채널 정보 DTO
        String id,
        String title,
        String thumbnailUrl,
        Long subscriberCount,
        Boolean isFavorited
) {
}
