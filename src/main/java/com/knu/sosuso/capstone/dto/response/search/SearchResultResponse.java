package com.knu.sosuso.capstone.dto.response.search;

import java.util.List;

public record SearchResultResponse(  // URL 검색 - 결과 개별 항목
        VideoResponse video,
        ChannelResponse channel,
        AnalysisResponse analysis,
        List<CommentResponse> comments
) {
}
