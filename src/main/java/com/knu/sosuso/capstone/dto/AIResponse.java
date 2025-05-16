package com.knu.sosuso.capstone.dto;

import java.util.List;

public record AIResponse(
        String summary,  // AI 전체 댓글 요약
        List<String> topKeywords,
        List<Float> languageRatio,  // e.g., [0.5, 0.3, 0.2]
        List<String> timeMentions,
        List<String> sentiment  // 각 댓글별 감정 (positive/negative/neutral)
) {
}