package com.knu.sosuso.capstone.dto;

import java.util.List;

public record SearchUnifiedResponse(
        VideoApiResponse videoInfo,
        List<CommentApiResponse.CommentData> commentInfo,
        AIResult aiResult
) {
}