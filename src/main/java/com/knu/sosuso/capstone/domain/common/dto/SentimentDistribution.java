package com.knu.sosuso.capstone.domain.common.dto;

/**
 * 감정 비율 공통 DTO
 */
public record SentimentDistribution(
        Integer positive,   // 긍정 비율
        Integer negative,   // 부정 비율
        Integer other       // 기타 비율
) {}