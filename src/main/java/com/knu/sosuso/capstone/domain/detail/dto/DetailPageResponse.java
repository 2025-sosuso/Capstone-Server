package com.knu.sosuso.capstone.domain.detail.dto;


import com.knu.sosuso.capstone.domain.comment.dto.CommentDto;

import java.util.List;

public record DetailPageResponse(
        DetailVideoDto video,
        DetailChannelDto channel,
        DetailAnalysisDto analysis,
        List<CommentDto> comments
) {
}
