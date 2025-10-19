package com.knu.sosuso.capstone.domain.detail.dto;

import java.util.List;

public record AIAnalysisResponse(
        String summary,
        Boolean isWarning,
        List<DetailAnalysisDto.LanguageDistribution> languageDistribution,
        DetailAnalysisDto.SentimentDistribution sentimentDistribution,
        List<String> keywords
) {
}