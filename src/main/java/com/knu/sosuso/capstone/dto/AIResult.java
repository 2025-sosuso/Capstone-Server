package com.knu.sosuso.capstone.dto;

import java.util.List;

public record AIResult(
        List<String> summary,  // AI 전체 댓글 요약
        List<EmotionResult> emotions,  // 각 댓글별 감정 (긍/부/중)
        List<String> languages,  // 언어 비율 정보 ("ko", "en" 등)
        List<String> keywords  // 주요 키워드 분석 결과
) {
    public record EmotionResult(
            String comment,  // 댓글 원문
            String label  // "긍정", "부정", "중립"
    ) {
    }
}