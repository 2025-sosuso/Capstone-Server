package com.knu.sosuso.capstone.domain.common.dto;

/**
 * 인기 시간 스탬프 공통 DTO
 */
public record PopularTimestamp(
        String time,
        Integer mentionCount
) {
}