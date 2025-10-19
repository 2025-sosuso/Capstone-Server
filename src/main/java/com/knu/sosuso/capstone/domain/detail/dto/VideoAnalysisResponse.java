package com.knu.sosuso.capstone.domain.detail.dto;

import java.util.List;

public record VideoAnalysisResponse(
        List<DetailAnalysisDto.CommentHistogram> commentHistogram,
        List<DetailAnalysisDto.PopularTimestamp> popularTimestamps,
        List<DetailCommentDto> topComments
) {
}