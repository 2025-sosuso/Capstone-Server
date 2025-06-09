package com.knu.sosuso.capstone.dto.response.detail;


import java.util.List;

public record DetailPageResponse(
        DetailVideoDto video,
        DetailChannelDto channel,
        DetailAnalysisDto analysis,
        List<DetailCommentDto> comments
) {
}
