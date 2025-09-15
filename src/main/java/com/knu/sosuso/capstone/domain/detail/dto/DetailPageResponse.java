package com.knu.sosuso.capstone.domain.detail.dto;


import java.util.List;

public record DetailPageResponse(
        DetailVideoDto video,
        DetailChannelDto channel,
        DetailAnalysisDto analysis,
        List<DetailCommentDto> comments
) {
}
