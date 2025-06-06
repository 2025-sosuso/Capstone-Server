package com.knu.sosuso.capstone.ai.dto;

import com.knu.sosuso.capstone.domain.enums.SentimentType;

import java.util.List;
import java.util.Map;

public record AIAnalysisResponse(
        Long videoId, // 영상 videoId
        String apiVideoId, // 영상 apiVideoId
        String summation, // 전체 댓글 요약
        boolean isWarning,
        List<String> keywords, // 댓글 주요 키워드
        Map<String, SentimentType> sentimentComments, // 댓글별 긍정, 부정, 기타 (Map<apiCommentId, commentContent>)
        Map<String, Double> languageRatio, // 언어 비율
        Map<String, Double> sentimentRatio // 전체 댓글 긍정, 부정, 기타 비율
) {
}
