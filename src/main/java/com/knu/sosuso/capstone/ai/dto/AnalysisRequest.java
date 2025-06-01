package com.knu.sosuso.capstone.ai.dto;

import java.util.Map;

public record AnalysisRequest(
        String videoId,
        Map<String, String> comments // Map<apiCommentId, commentContent>
) {
}
