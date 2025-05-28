package com.knu.sosuso.capstone.dto.response;

public record SearchUrlResponse(
        VideoApiResponse videoInfo,
        CommentApiResponse commentInfo
) {
}