package com.knu.sosuso.capstone.domain.video.entity.value;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * AI 분석 상태 관리
 * 비디오의 AI 분석 진행 상태를 추적하기 위한 enum
 */
@Getter
@RequiredArgsConstructor
public enum AIAnalysisStatus {

    /**
     * 대기 중 - AI 분석이 아직 시작되지 않음
     */
    PENDING("대기 중", "AI 분석 대기 중"),

    /**
     * 처리 중 - AI 분석이 진행 중
     */
    IN_PROGRESS("처리 중", "AI 분석 진행 중"),

    /**
     * 완료 - AI 분석이 성공적으로 완료됨
     */
    COMPLETED("완료", "AI 분석 완료"),

    /**
     * 실패 - AI 분석이 실패함 (재시도 가능)
     */
    FAILED("실패", "AI 분석 실패"),

    /**
     * 부분 완료 - 일부 분석만 완료됨
     */
    PARTIAL("부분 완료", "일부 분석 완료"),

    /**
     * 건너뜀 - 댓글이 없거나 비활성화되어 분석 건너뜀
     */
    SKIPPED("건너뜀", "AI 분석 건너뜀"),

    /**
     * 재시도 중 - 실패 후 재시도 중
     */
    RETRYING("재시도 중", "AI 분석 재시도 중");

    private final String korean;
    private final String description;

    /**
     * AI 분석이 완료된 상태인지 확인
     */
    public boolean isCompleted() {
        return this == COMPLETED || this == PARTIAL;
    }

    /**
     * 재시도가 가능한 상태인지 확인
     */
    public boolean isRetryable() {
        return this == FAILED || this == PARTIAL;
    }

    /**
     * 처리 중인 상태인지 확인
     */
    public boolean isProcessing() {
        return this == IN_PROGRESS || this == RETRYING;
    }
}