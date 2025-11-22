package com.knu.sosuso.capstone.domain.detail.dto;

import com.knu.sosuso.capstone.domain.comment.dto.CommentDto;
import com.knu.sosuso.capstone.domain.common.dto.CommentHistogram;
import com.knu.sosuso.capstone.domain.common.dto.PopularTimestamp;
import com.knu.sosuso.capstone.domain.common.dto.SentimentFlow;

import java.util.List;

public record VideoAnalysisResponse(
        List<CommentHistogram> commentHistogram,
        List<PopularTimestamp> popularTimestamps,
        List<CommentDto> topComments,
        List<SentimentFlow> sentimentFlow
) {
}