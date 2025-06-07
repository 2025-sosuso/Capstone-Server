package com.knu.sosuso.capstone.dto.response.comment;

import java.util.List;

public record CommentSearchResponse(
        Long videoId,
        List<CommentSearchDto> results
) {
}

