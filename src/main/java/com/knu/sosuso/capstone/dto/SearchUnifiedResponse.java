package com.knu.sosuso.capstone.dto;

public record SearchUnifiedResponse(
        VideoApiResponse videoInfo,
        CommentApiResponse commentInfo
) {
}