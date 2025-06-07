package com.knu.sosuso.capstone.dto.response.search;

import java.util.List;

public record AnalysisResponse(  // URL 검색 - 분석 정보 DTO
        String summary,
        Boolean isWarning,
        List<CommentResponse> topComments,
        List<LanguageDistribution> languageDistribution,
        SentimentDistribution sentimentDistribution,
        List<PopularTimestamp> popularTimestamps,
        List<CommentHistogram> commentHistogram,
        List<String> keywords
) {
    public record LanguageDistribution(
            String language,
            Double ratio
    ) {}

    public record SentimentDistribution(
            Double positive,
            Double negative,
            Double other
    ) {}

    public record PopularTimestamp(
            String time,
            Integer mentionCount
    ) {}

    public record CommentHistogram(
            String hour,
            Integer count
    ) {}
}
