package com.knu.sosuso.capstone.dto.response.search;

public record VideoResponse(  // URL 검색 - 비디오 정보 DTO
        String id,
        String title,
        String description,
        String publishedAt,
        String thumbnailUrl,
        Long viewCount,
        Long likeCount,
        Integer commentCount,
        Boolean isScrapped,
        Long scrapId
) {
}
