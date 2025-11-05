package com.knu.sosuso.capstone.domain.detail.dto;

import com.knu.sosuso.capstone.domain.comment.dto.CommentDto;

import java.util.List;

public record VideoAnalysisResponse(
        List<DetailAnalysisDto.CommentHistogram> commentHistogram,
        List<DetailAnalysisDto.PopularTimestamp> popularTimestamps,
        List<CommentDto> topComments
) {
}