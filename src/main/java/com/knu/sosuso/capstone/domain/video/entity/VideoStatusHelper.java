package com.knu.sosuso.capstone.domain.video.entity;

import com.knu.sosuso.capstone.domain.video.entity.value.AIAnalysisStatus;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

/**
 * Video 엔티티의 AI 분석 상태 관리를 위한 헬퍼 클래스
 * Video 엔티티 내부에 메서드로 추가하거나 별도 유틸리티로 사용 가능
 */
@Slf4j
public class VideoStatusHelper {

    /**
     * AI 분석 시작 - 상태를 IN_PROGRESS로 변경
     */
    public static void startAIAnalysis(Video video) {
        if (video.getAiAnalysisStatus() == AIAnalysisStatus.IN_PROGRESS) {
            log.warn("이미 AI 분석 중인 비디오: apiVideoId={}", video.getApiVideoId());
            return;
        }

        video.setAiProcessing(true);
        video.setAiAnalysisStatus(AIAnalysisStatus.IN_PROGRESS);
        video.setLastAiAttemptAt(LocalDateTime.now());

        log.info("AI 분석 시작: apiVideoId={}, status={}",
                video.getApiVideoId(), video.getAiAnalysisStatus());
    }

    /**
     * AI 분석 완료 - 상태를 COMPLETED로 변경
     */
    public static void completeAIAnalysis(Video video) {
        video.setAiProcessing(false);
        video.setAiAnalysisStatus(AIAnalysisStatus.COMPLETED);
        video.setAiRetryCount(0);  // 성공 시 재시도 카운트 리셋

        log.info("AI 분석 완료: apiVideoId={}", video.getApiVideoId());
    }

    /**
     * AI 분석 실패 - 상태를 FAILED로 변경하고 재시도 카운트 증가
     */
    public static void failAIAnalysis(Video video, String errorMessage) {
        video.setAiProcessing(false);
        video.setAiAnalysisStatus(AIAnalysisStatus.FAILED);
        video.setAiRetryCount(video.getAiRetryCount() + 1);
        video.setLastAiAttemptAt(LocalDateTime.now());

        log.error("AI 분석 실패: apiVideoId={}, retryCount={}, error={}",
                video.getApiVideoId(), video.getAiRetryCount(), errorMessage);
    }

    /**
     * AI 분석 재시도 - 상태를 RETRYING으로 변경
     */
    public static void retryAIAnalysis(Video video) {
        if (!video.getAiAnalysisStatus().isRetryable()) {
            log.warn("재시도 불가능한 상태: apiVideoId={}, status={}",
                    video.getApiVideoId(), video.getAiAnalysisStatus());
            return;
        }

        video.setAiProcessing(true);
        video.setAiAnalysisStatus(AIAnalysisStatus.RETRYING);
        video.setAiRetryCount(video.getAiRetryCount() + 1);
        video.setLastAiAttemptAt(LocalDateTime.now());

        log.info("AI 분석 재시도: apiVideoId={}, retryCount={}",
                video.getApiVideoId(), video.getAiRetryCount());
    }

    /**
     * AI 분석 건너뛰기 - 댓글이 없거나 비활성화된 경우
     */
    public static void skipAIAnalysis(Video video, String reason) {
        video.setAiProcessing(false);
        video.setAiAnalysisStatus(AIAnalysisStatus.SKIPPED);

        log.info("AI 분석 건너뛰기: apiVideoId={}, reason={}",
                video.getApiVideoId(), reason);
    }

    /**
     * 부분 완료 - 일부 분석만 성공한 경우
     */
    public static void partialCompleteAIAnalysis(Video video) {
        video.setAiProcessing(false);
        video.setAiAnalysisStatus(AIAnalysisStatus.PARTIAL);

        log.warn("AI 분석 부분 완료: apiVideoId={}", video.getApiVideoId());
    }

    /**
     * AI 분석이 필요한지 확인
     */
    public static boolean needsAIAnalysis(Video video) {
        // 댓글이 비활성화되거나 없으면 분석 불필요
        if (video.isCommentsDisabled() || video.isHasNoComments()) {
            return false;
        }

        // 이미 완료되었으면 분석 불필요
        if (video.getAiAnalysisStatus() == AIAnalysisStatus.COMPLETED) {
            return false;
        }

        // 현재 처리 중이면 분석 불필요
        if (video.getAiAnalysisStatus().isProcessing()) {
            return false;
        }

        // PENDING, FAILED, PARTIAL 상태는 분석 필요
        return true;
    }

    /**
     * 재시도 가능 여부 확인 (최대 3회)
     */
    public static boolean canRetry(Video video, int maxRetryCount) {
        return video.getAiAnalysisStatus().isRetryable()
                && video.getAiRetryCount() < maxRetryCount;
    }
}