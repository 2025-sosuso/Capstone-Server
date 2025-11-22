package com.knu.sosuso.capstone.domain.detail.dto;

import com.knu.sosuso.capstone.domain.common.dto.LanguageDistribution;
import com.knu.sosuso.capstone.domain.common.dto.SentimentDistribution;

import java.util.List;

public record AIAnalysisResponse(
        String summary,
        Boolean isWarning,
        List<LanguageDistribution> languageDistribution,
        SentimentDistribution sentimentDistribution,
        List<String> keywords
) {
}