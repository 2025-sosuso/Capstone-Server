package com.knu.sosuso.capstone.domain.video.entity;

/**
 * AI 분석 상태
 */
public enum AIAnalysisStatus {
    PENDING,    // 분석 대기 중 (초기 상태)
    COMPLETED,  // 분석 완료
    SKIPPED;    // 댓글 없어서 스킵
}