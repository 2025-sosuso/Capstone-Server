package com.knu.sosuso.capstone.global.scheduler;

import com.knu.sosuso.capstone.domain.video.service.PopularVideoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 인기도 관련 스케줄링
 *
 * 1. 매 시간마다 인기도 점수 재계산
 * 2. 매일 새벽 3시에 오래된 로그 삭제
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class PopularityScheduler {

    private final PopularVideoService popularVideoService;

    /**
     * 매 시간 정각에 인기도 점수 재계산
     * cron: 초 분 시 일 월 요일
     * "0 0 * * * *" = 매 시간 00분 00초
     */
    @Scheduled(cron = "0 0 * * * *")
    public void calculatePopularity() {
        log.info("⏰ [스케줄] 인기도 점수 계산 시작");

        try {
            int updatedCount = popularVideoService.recalculatePopularity();
            log.info("✅ [스케줄] 인기도 점수 계산 완료: {}개 영상 업데이트", updatedCount);

        } catch (Exception e) {
            log.error("❌ [스케줄] 인기도 점수 계산 실패", e);
        }
    }

    /**
     * 매일 새벽 3시에 오래된 로그 삭제
     * cron: "0 0 3 * * *" = 매일 03:00:00
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void cleanupLogs() {
        log.info("🗑️ [스케줄] 오래된 조회 로그 삭제 시작");

        try {
            popularVideoService.cleanupOldLogs();
            log.info("✅ [스케줄] 오래된 조회 로그 삭제 완료");

        } catch (Exception e) {
            log.error("❌ [스케줄] 로그 삭제 실패", e);
        }
    }
}