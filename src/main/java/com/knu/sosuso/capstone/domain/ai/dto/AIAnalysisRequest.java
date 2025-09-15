package com.knu.sosuso.capstone.domain.ai.dto;

import java.util.Map;

public record AIAnalysisRequest(
        String videoId,
        Map<String, String> comments // Map<apiCommentId, commentContent>
) {
}
