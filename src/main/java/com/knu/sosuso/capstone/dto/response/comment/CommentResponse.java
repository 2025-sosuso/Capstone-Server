package com.knu.sosuso.capstone.dto.response.comment;

import java.util.List;

public record CommentResponse(
        String apiVideoId,
        List<CommentDto> results
) {
}
