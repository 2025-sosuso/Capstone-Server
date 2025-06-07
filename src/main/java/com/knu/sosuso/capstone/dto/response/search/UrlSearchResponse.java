package com.knu.sosuso.capstone.dto.response.search;

import java.util.List;

public record UrlSearchResponse(
        UrlVideoDto video,
        UrlChannelDto channel,
        UrlAnalysisDto analysis,
        List<UrlCommentDto> comments
) {
}
