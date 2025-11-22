package com.knu.sosuso.capstone.domain.common.dto;

/**
 * 언이 비율 공통 DTO
 */
public record LanguageDistribution(
        String language,
        Integer ratio
) {
}