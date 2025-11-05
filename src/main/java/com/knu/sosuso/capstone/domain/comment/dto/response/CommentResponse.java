package com.knu.sosuso.capstone.domain.comment.dto.response;

import com.knu.sosuso.capstone.domain.comment.dto.CommentDto;

import java.util.List;

public record CommentResponse(
        String apiVideoId,
        List<CommentDto> results
) {
}
