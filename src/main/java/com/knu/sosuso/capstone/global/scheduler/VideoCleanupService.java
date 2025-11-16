package com.knu.sosuso.capstone.global.scheduler;

import com.knu.sosuso.capstone.domain.video.entity.Video;
import com.knu.sosuso.capstone.domain.video.repository.VideoRepository;
import com.knu.sosuso.capstone.domain.scrap.repository.ScrapRepository;
import com.knu.sosuso.capstone.domain.comment.repository.CommentRepository;
import com.knu.sosuso.capstone.domain.video.service.VideoProcessingService;
import com.knu.sosuso.capstone.global.config.AppConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 비디오 데이터 정리 배치 작업
 * - 삭제된 영상의 하드 삭제 (스크랩 보존)
 * - 스크랩된 영상의 메타데이터 갱신
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class VideoCleanupService {

    private final VideoRepository videoRepository;
    private final ScrapRepository scrapRepository;
    private final CommentRepository commentRepository;
    private final AppConfig appConfig;
    private final VideoProcessingService videoProcessingService;

    /**
     * 오래된 삭제 영상 하드 삭제
     * 매달 1일 새벽 3시에 실행
     */
    @Scheduled(cron = "0 0 3 1 * *")  // 매달 1일 03:00
    @Transactional
    public void cleanupOldDeletedVideos() {
        log.info("=== 오래된 삭제 영상 정리 시작 ===");

        LocalDateTime retentionThreshold = LocalDateTime.now()
                .minusDays(appConfig.getDataRetentionDays());

        List<Video> oldDeletedVideos = videoRepository
                .findByDeletedTrueAndDeleteCheckedAtBefore(retentionThreshold);

        if (oldDeletedVideos.isEmpty()) {
            log.info("정리할 삭제 영상 없음");
            return;
        }

        log.info("삭제 후보 영상 수: {}개", oldDeletedVideos.size());

        int successCount = 0;
        int failCount = 0;
        int skippedCount = 0;

        for (Video video : oldDeletedVideos) {
            try {
                // 스크랩 여부 확인
                boolean hasScrap = scrapRepository.existsByVideoId(video.getId());

                if (hasScrap) {
                    log.info("스크랩된 영상이므로 유지: videoId={}, apiVideoId={}",
                            video.getId(), video.getApiVideoId());
                    skippedCount++;
                    continue;
                }

                log.info("영상 하드 삭제 시작: videoId={}, apiVideoId={}, 삭제확인일={}",
                        video.getId(), video.getApiVideoId(), video.getDeleteCheckedAt());

                // 1. 관련 댓글 삭제
                commentRepository.deleteByVideoId(video.getId());
                log.info("댓글 삭제 완료: videoId={}", video.getId());

                // 2. 영상 삭제
                videoRepository.delete(video);

                log.info("영상 하드 삭제 완료: videoId={}, apiVideoId={}",
                        video.getId(), video.getApiVideoId());

                successCount++;

            } catch (Exception e) {
                failCount++;
                log.error("영상 삭제 실패: videoId={}, apiVideoId={}, error={}",
                        video.getId(), video.getApiVideoId(), e.getMessage(), e);
            }
        }

        log.info("=== 오래된 삭제 영상 정리 완료 ===");
        log.info("성공: {}개, 스킵(스크랩됨): {}개, 실패: {}개, 전체: {}개",
                successCount, skippedCount, failCount, oldDeletedVideos.size());
    }

    /**
     * 스크랩된 영상의 메타데이터 정기 갱신
     * 매일 새벽 2시에 실행
     */
    @Scheduled(cron = "0 0 2 * * *")  // 매일 02:00
    @Transactional
    public void updateScrappedVideosMetadata() {
        log.info("=== 스크랩된 영상 메타데이터 갱신 시작 ===");

        LocalDateTime updateThreshold = LocalDateTime.now()
                .minusDays(appConfig.getMetadataUpdateDays());

        List<Video> videosToUpdate = videoRepository
                .findVideosNeedingMetadataUpdate(updateThreshold);

        if (videosToUpdate.isEmpty()) {
            log.info("갱신할 영상 없음");
            return;
        }

        log.info("갱신 대상 영상 수: {}개", videosToUpdate.size());

        int scheduledCount = 0;

        for (Video video : videosToUpdate) {
            try {
                log.info("📊 메타데이터 갱신 스케줄: videoId={}, apiVideoId={}",
                        video.getId(), video.getApiVideoId());

                // updateMetadata 호출
                videoProcessingService.updateMetadata(
                        video.getId(),
                        video.getApiVideoId()
                );

                scheduledCount++;

            } catch (Exception e) {
                log.error("❌ 메타데이터 갱신 실패: videoId={}, error={}",
                        video.getId(), e.getMessage());
            }
        }

        log.info("=== 스크랩된 영상 메타데이터 갱신 완료 ===");
        log.info("성공: {}개 / 전체: {}개", scheduledCount, videosToUpdate.size());
    }
}