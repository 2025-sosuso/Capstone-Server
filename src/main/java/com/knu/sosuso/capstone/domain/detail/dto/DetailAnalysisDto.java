package com.knu.sosuso.capstone.domain.detail.dto;

import com.knu.sosuso.capstone.domain.comment.dto.CommentDto;

import java.util.List;

public record DetailAnalysisDto(
        String summary,
        Boolean isWarning,
        List<CommentDto> topComments,
        List<DetailAnalysisDto.LanguageDistribution> languageDistribution,
        DetailAnalysisDto.SentimentDistribution sentimentDistribution,
        List<DetailAnalysisDto.PopularTimestamp> popularTimestamps,
        List<DetailAnalysisDto.CommentHistogram> commentHistogram,
        List<String> keywords
) {
    public record LanguageDistribution(
            String language,
            Double ratio
    ) {
    }

    public record SentimentDistribution(
            Double positive,
            Double negative,
            Double other
    ) {
    }

    public record PopularTimestamp(
            String time,
            Integer mentionCount
    ) {
    }

    public record CommentHistogram(
            String hour,
            Integer count
    ) {
    }
}