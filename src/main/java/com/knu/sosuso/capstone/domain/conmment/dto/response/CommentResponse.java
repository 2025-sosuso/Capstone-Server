package com.knu.sosuso.capstone.domain.conmment.dto.response;

import com.knu.sosuso.capstone.domain.conmment.dto.CommentDto;

import java.util.List;

public record CommentResponse(
        String apiVideoId,
        List<CommentDto> results
) {
}
