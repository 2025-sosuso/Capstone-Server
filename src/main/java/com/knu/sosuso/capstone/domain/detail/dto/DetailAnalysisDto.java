package com.knu.sosuso.capstone.domain.detail.dto;

import com.knu.sosuso.capstone.domain.comment.dto.CommentDto;
import com.knu.sosuso.capstone.domain.common.dto.CommentHistogram;
import com.knu.sosuso.capstone.domain.common.dto.LanguageDistribution;
import com.knu.sosuso.capstone.domain.common.dto.PopularTimestamp;
import com.knu.sosuso.capstone.domain.common.dto.SentimentDistribution;

import java.util.List;

public record DetailAnalysisDto(
        String summary,
        Boolean isWarning,
        List<CommentDto> topComments,
        List<LanguageDistribution> languageDistribution,
        SentimentDistribution sentimentDistribution,
        List<PopularTimestamp> popularTimestamps,
        List<CommentHistogram> commentHistogram,
        List<String> keywords
) {


}