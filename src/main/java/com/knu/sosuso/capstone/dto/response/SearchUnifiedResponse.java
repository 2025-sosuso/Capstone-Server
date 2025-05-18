package com.knu.sosuso.capstone.dto.response;

public record SearchUnifiedResponse(
        VideoApiResponse videoInfo,
        CommentApiResponse commentInfo
) {
}