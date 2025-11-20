package com.knu.sosuso.capstone.domain.common.dto;

/**
 * 영상 기본 정보 공통 DTO
 */
public record VideoBasicDto(
        String id,
        String title,
        String description,
        String publishedAt,
        String thumbnailUrl,
        Long viewCount,
        Long likeCount,
        Integer commentCount
) {}