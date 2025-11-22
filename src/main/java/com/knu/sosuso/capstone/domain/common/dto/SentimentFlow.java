package com.knu.sosuso.capstone.domain.common.dto;

/**
 * 감정 흐름 공통 DTO
 */
public record SentimentFlow(
        String date,
        Integer positive,
        Integer negative,
        Integer other
) {
}
