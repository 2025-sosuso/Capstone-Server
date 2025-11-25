package com.knu.sosuso.capstone.domain.video.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knu.sosuso.capstone.domain.ai.dto.AIAnalysisRequest;
import com.knu.sosuso.capstone.domain.ai.dto.AIAnalysisResponse;
import com.knu.sosuso.capstone.domain.ai.service.AnalysisService;
import com.knu.sosuso.capstone.domain.comment.entity.Comment;
import com.knu.sosuso.capstone.domain.comment.service.CommentService;
import com.knu.sosuso.capstone.domain.comment.dto.response.CommentApiResponse;
import com.knu.sosuso.capstone.domain.comment.repository.CommentRepository;
import com.knu.sosuso.capstone.domain.video.dto.response.VideoSummaryResponse;
import com.knu.sosuso.capstone.domain.video.entity.AIAnalysisStatus;
import com.knu.sosuso.capstone.domain.video.entity.Video;
import com.knu.sosuso.capstone.domain.video.dto.response.VideoApiResponse;
import com.knu.sosuso.capstone.domain.video.entity.VideoType;
import com.knu.sosuso.capstone.domain.video.repository.VideoRepository;
import com.knu.sosuso.capstone.global.config.AppConfig;
import com.knu.sosuso.capstone.global.exception.BusinessException;
import com.knu.sosuso.capstone.global.exception.error.CommonError;
import com.knu.sosuso.capstone.global.exception.error.VideoError;
import com.knu.sosuso.capstone.global.service.mapper.VideoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 비디오 처리 서비스
 * - 검색 시: AI 없이 즉시 응답
 * - 스케줄러: 주기적으로 미분석 영상 AI 처리
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class VideoProcessingService {

    private final VideoService videoService;
    private final CommentService commentService;
    private final AnalysisService analysisService;
    private final CommentRepository commentRepository;
    private final VideoRepository videoRepository;
    private final VideoMapper videoMapper;
    private final CacheManager cacheManager;
    private final AppConfig appConfig;
    private final UserDataService userDataService;
    private final VideoTypeDetector videoTypeDetector;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ========================================
    // 1. 공통 핵심 로직: 새 영상 처리
    // ========================================

    /**
     * 새 영상 처리 (공통 로직)
     * - VideoDetailService와 검색 페이지 모두 사용
     *
     * @return Video 엔티티
     */
    @Transactional
    public Video processNewVideo(String apiVideoId, VideoType expectedType) {
        log.info("🆕 새 영상 처리: apiVideoId={}, expectedType={}", apiVideoId, expectedType);

        try {
            // 1. YouTube API로 메타데이터 가져오기
            VideoApiResponse videoInfo = videoService.getVideoInfo(apiVideoId);

            // 2. 실제 타입 판별 (URL + 썸네일 비율)
            VideoType actualType = videoTypeDetector.detectVideoType(
                    apiVideoId,
                    videoInfo.thumbnails() // JsonNode
            );

            // 3. 타입 불일치 로그
            if (expectedType != actualType) {
                log.warn("⚠️ 영상 타입 불일치: apiVideoId={}, expected={}, actual={}",
                        apiVideoId, expectedType, actualType);
            }

            log.info("📹 영상 타입 확정: apiVideoId={}, type={}", apiVideoId, actualType);

            // 4. 댓글 수집
            CommentApiResponse commentResponse = commentService.getComments(apiVideoId);

            Video video;

            if (commentResponse.allComments() == null || commentResponse.allComments().isEmpty()) {
                log.info("💬 댓글 없는 영상: apiVideoId={}", apiVideoId);

                video = videoService.saveVideoMetadataOnly(videoInfo, actualType);
                video.setAiAnalysisStatus(AIAnalysisStatus.SKIPPED);
                video.setHasNoComments(true);
                videoRepository.save(video);

                return video;
            }

            log.info("💬 댓글 수집 완료: {}개", commentResponse.allComments().size());

            List<Comment> comments = commentService.createCommentsWithoutAnalysis(
                    commentResponse.allComments()
            );

            video = videoService.saveVideoWithAnalysis(videoInfo, commentResponse, actualType);
            videoRepository.flush();

            for (Comment comment : comments) {
                comment.setVideo(video);
            }
            commentRepository.saveAll(comments);

            log.info("✅ Video + Comment 저장 완료: videoId={}, type={}, 댓글={}개",
                    video.getId(), actualType, comments.size());

            return video;

        } catch (Exception e) {
            log.error("❌ 새 영상 처리 실패: apiVideoId={}", apiVideoId, e);
            throw new BusinessException(VideoError.VIDEO_PROCESSING_ERROR);
        }
    }

    // ========================================
    // 2. 검색용 래퍼 메서드 (트랜잭션 최적화)
    // ========================================

    /**
     * 비디오 처리 메인 진입점 (검색용)
     */
    @Transactional
    public Video processVideoToSearchResult(String apiVideoId) {
        if (apiVideoId == null || apiVideoId.trim().isEmpty()) {
            throw new BusinessException(VideoError.VIDEO_ID_REQUIRED);
        }

        log.info("비디오 처리 시작: apiVideoId={}", apiVideoId);

        Optional<Video> existingVideo = videoRepository.findByApiVideoId(apiVideoId);

        if (existingVideo.isPresent()) {
            return handleExistingVideoWithTransaction(existingVideo.get());
        } else {
            // 쓰기 작업은 별도 트랜잭션
            return processNewVideo(apiVideoId, VideoType.VIDEO);
        }
    }

    /**
     * 기존 영상 처리 (검색용) - 쓰기 작업 분리
     */
    @Transactional
    public Video handleExistingVideoWithTransaction(Video video) {
        String apiVideoId = video.getApiVideoId();

        // 삭제 확인
        if (shouldCheckDeletion(video)) {
            boolean isDeleted = videoService.checkIfVideoDeleted(apiVideoId);
            if (isDeleted) {
                video.setDeleted(true);
                video.setDeleteCheckedAt(LocalDateTime.now());
                videoRepository.save(video);
                throw new BusinessException(VideoError.VIDEO_DELETED);
            }
            video.setDeleteCheckedAt(LocalDateTime.now());
            videoRepository.save(video);
        }

        // 메타데이터 갱신
        if (shouldUpdateMetadata(video)) {
            log.info("메타데이터 갱신 필요: apiVideoId={}", apiVideoId);
            updateMetadataInternal(video);
        }

        return video;
    }

    /**
     * 비디오 타입 업데이트 (쓰기 작업 분리)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public VideoSummaryResponse updateVideoTypeAndReturnSummary(
            String token, Video video, VideoType confirmedType) {
        video.setVideoType(confirmedType);
        videoRepository.save(video);

        Long scrapId = getScrapIdSafely(token, video.getApiVideoId());
        return videoMapper.toSummaryResponse(video, scrapId);
    }

    // ========================================
    // 3. 스케줄러: 주기적 AI 분석
    // ========================================

    /**
     * 스케줄러: 1분마다 미분석 영상 처리
     * - PENDING 상태 영상 최대 10개
     * - 최근 조회순 우선
     */
    @Scheduled(fixedDelayString = "#{${ai.batch.interval.ms:60000}}")
    public void processUnanalyzedVideos() {
        log.info("🔄 미분석 영상 배치 처리 시작");

        try {
            int batchSize = appConfig.getAiBatchSize();

            // ✅ 읽기 전용 트랜잭션으로 조회
            List<Video> videos = findPendingVideosReadOnly();

            if (videos.isEmpty()) {
                log.debug("처리할 미분석 영상 없음");
                return;
            }

            // 설정된 개수만큼만 처리
            List<Video> targetVideos = videos.stream()
                    .limit(batchSize)
                    .toList();

            log.info("📋 처리 대상: {}개", targetVideos.size());

            int successCount = 0;
            int skipCount = 0;
            int failCount = 0;

            for (Video video : targetVideos) {
                try {
                    // ✅ 각 영상 처리는 별도 트랜잭션
                    ProcessResult result = processAIAnalysis(video);

                    switch (result) {
                        case SUCCESS -> successCount++;
                        case SKIPPED -> skipCount++;
                        case FAILED -> failCount++;
                    }

                } catch (Exception e) {
                    log.error("❌ AI 분석 중 예외: videoId={}, error={}",
                            video.getId(), e.getMessage());
                    failCount++;
                }
            }

            log.info("✅ 배치 완료: 성공={}, 스킵={}, 실패={}, 전체={}",
                    successCount, skipCount, failCount, targetVideos.size());

        } catch (Exception e) {
            log.error("❌ 배치 처리 중 오류: {}", e.getMessage(), e);
        }
    }

    /**
     * PENDING 영상 조회 (읽기 전용)
     */
    @Transactional(readOnly = true)
    public List<Video> findPendingVideosReadOnly() {
        return videoRepository
                .findTop10ByAiAnalysisStatusOrderByUpdatedAtDesc(AIAnalysisStatus.PENDING);
    }

    /**
     * 개별 영상 AI 분석
     */
    @Transactional
    public ProcessResult processAIAnalysis(Video video) {
        log.info("🎬 AI 분석 시작: videoId={}, apiVideoId={}",
                video.getId(), video.getApiVideoId());

        try {
            // 1. 댓글 조회
            List<Comment> comments = commentRepository.findByVideo(video);

            if (comments.isEmpty()) {
                log.warn("⚠️ 댓글 없음, 스킵: videoId={}", video.getId());
                video.setAiAnalysisStatus(AIAnalysisStatus.SKIPPED);
                videoRepository.save(video);
                return ProcessResult.SKIPPED;
            }

            // 2. AI 분석 요청
            Map<String, String> commentsForAI = comments.stream()
                    .collect(Collectors.toMap(
                            Comment::getApiCommentId,
                            Comment::getCommentContent,
                            (v1, v2) -> v1,
                            LinkedHashMap::new
                    ));

            AIAnalysisRequest request = new AIAnalysisRequest(
                    video.getApiVideoId(), commentsForAI
            );

            AIAnalysisResponse response = analysisService.requestAnalysis(request);

            if (response == null || response.summation() == null ||
                    response.summation().trim().isEmpty()) {
                log.error("❌ AI 응답 비어있음: videoId={}", video.getId());
                return ProcessResult.FAILED;
            }

            // 3. Video 업데이트
            log.info("💾 Video 업데이트 시작");

            if (response.summation() != null && !response.summation().trim().isEmpty()) {
                video.setSummation(response.summation());
            }

            video.setWarning(response.isWarning());

            if (response.languageRatio() != null && !response.languageRatio().isEmpty()) {
                String languageJson = objectMapper.writeValueAsString(response.languageRatio());
                video.setLanguageDistribution(languageJson);
            }

            if (response.sentimentRatio() != null && !response.sentimentRatio().isEmpty()) {
                String sentimentJson = objectMapper.writeValueAsString(response.sentimentRatio());
                video.setSentimentDistribution(sentimentJson);
            }

            if (response.keywords() != null && !response.keywords().isEmpty()) {
                String keywordsJson = objectMapper.writeValueAsString(response.keywords());
                video.setKeywords(keywordsJson);
            }

            video.setAiAnalysisStatus(AIAnalysisStatus.COMPLETED);
            videoRepository.save(video);

            log.info("✅ Video 저장 완료");

            // 4. Comment 업데이트
            log.info("💾 Comment 업데이트 시작");
            commentService.updateCommentsWithAnalysis(response);
            log.info("✅ Comment 저장 완료");

            // 5. 캐시 삭제
            evictVideoCache(video.getApiVideoId());

            log.info("✅ AI 분석 성공: videoId={}, apiVideoId={}",
                    video.getId(), video.getApiVideoId());

            return ProcessResult.SUCCESS;

        } catch (Exception e) {
            log.error("❌ AI 분석 실패: videoId={}, error={}",
                    video.getId(), e.getMessage(), e);
            return ProcessResult.FAILED;
        }
    }

    /**
     * 처리 결과
     */
    private enum ProcessResult {
        SUCCESS,  // 성공
        SKIPPED,  // 댓글 없어서 스킵
        FAILED    // 실패 (다음에 재시도)
    }

    // ========================================
    // 4. 헬퍼 메서드
    // ========================================

    /**
     * 삭제 확인 필요 여부
     */
    private boolean shouldCheckDeletion(Video video) {
        if (video.isDeleted()) {
            return false;
        }
        if (video.getDeleteCheckedAt() == null) {
            return true;
        }
        LocalDateTime checkThreshold = LocalDateTime.now()
                .minusDays(appConfig.getDeletionCheckDays());
        return video.getDeleteCheckedAt().isBefore(checkThreshold);
    }

    /**
     * 메타데이터 갱신 필요 여부
     */
    private boolean shouldUpdateMetadata(Video video) {
        if (video.getLastMetadataUpdatedAt() == null) {
            return true;
        }
        LocalDateTime updateThreshold = LocalDateTime.now()
                .minusDays(appConfig.getMetadataUpdateDays());
        return video.getLastMetadataUpdatedAt().isBefore(updateThreshold);
    }

    /**
     * 메타데이터만 업데이트 (비동기)
     * - 조회수, 좋아요, 댓글 수 등만 갱신
     * - AI 분석은 하지 않음
     */
    @Async("videoProcessingExecutor")
    @Transactional
    public void updateMetadata(Long videoId, String apiVideoId) {
        log.info("📊 메타데이터 갱신 시작: videoId={}, apiVideoId={}", videoId, apiVideoId);

        try {
            // 1. YouTube API에서 최신 정보 가져오기
            VideoApiResponse latestInfo = videoService.getVideoInfo(apiVideoId);

            // 2. DB에서 비디오 조회
            Video video = videoRepository.findById(videoId)
                    .orElseThrow(() -> new BusinessException(VideoError.VIDEO_NOT_FOUND));

            // 3. 메타데이터만 업데이트
            video.setTitle(latestInfo.title());
            video.setDescription(latestInfo.description());
            video.setViewCount(latestInfo.viewCount());
            video.setLikeCount(latestInfo.likeCount());
            video.setCommentCount(latestInfo.commentCount());
            video.setThumbnailUrl(latestInfo.thumbnailUrl());
            video.setSubscriberCount(latestInfo.subscriberCount());

            // 갱신 시간 기록
            video.setLastMetadataUpdatedAt(LocalDateTime.now());
            video.setMetadataUpdateCount(video.getMetadataUpdateCount() + 1);

            videoRepository.save(video);

            log.info("✅ 메타데이터 갱신 완료: videoId={}, 갱신 횟수={}",
                    videoId, video.getMetadataUpdateCount());

        } catch (BusinessException e) {
            if (e.getError() == VideoError.VIDEO_NOT_FOUND) {
                // YouTube에서 삭제됨
                log.warn("⚠️ YouTube에서 영상 삭제됨: apiVideoId={}", apiVideoId);

                try {
                    Video video = videoRepository.findById(videoId).orElse(null);
                    if (video != null) {
                        video.setDeleted(true);
                        video.setDeleteCheckedAt(LocalDateTime.now());
                        videoRepository.save(video);
                        log.info("🗑️ 영상 삭제 플래그 설정: videoId={}", videoId);
                    }
                } catch (Exception ex) {
                    log.error("❌ 삭제 플래그 설정 실패: {}", ex.getMessage());
                }
            } else {
                throw e;
            }
        } catch (Exception e) {
            log.error("❌ 메타데이터 갱신 실패: videoId={}, apiVideoId={}, error={}",
                    videoId, apiVideoId, e.getMessage(), e);
            throw new BusinessException(CommonError.VIDEO_PROCESSING_ERROR);
        }
    }

    /**
     * 메타데이터 갱신 (내부용, 이미 트랜잭션 안)
     */
    private void updateMetadataInternal(Video video) {
        try {
            VideoApiResponse videoInfo = videoService.getVideoInfo(video.getApiVideoId());

            video.setViewCount(videoInfo.viewCount());
            video.setLikeCount(videoInfo.likeCount());
            video.setCommentCount(videoInfo.commentCount());
            video.setLastMetadataUpdatedAt(LocalDateTime.now());
            video.setMetadataUpdateCount(video.getMetadataUpdateCount() + 1);

            videoRepository.save(video);

            log.info("메타데이터 갱신 완료: apiVideoId={}", video.getApiVideoId());

        } catch (Exception e) {
            log.error("메타데이터 갱신 실패: apiVideoId={}", video.getApiVideoId(), e);
        }
    }

    /**
     * 비디오 캐시 무효화
     */
    private void evictVideoCache(String apiVideoId) {
        try {
            Cache cache = cacheManager.getCache("videoDetail");
            if (cache != null) {
                cache.evict("video-" + apiVideoId);
                cache.evict("analysis-" + apiVideoId);
                cache.evict("ai-" + apiVideoId);
                cache.evict(apiVideoId);

                log.debug("비디오 캐시 무효화: apiVideoId={}", apiVideoId);
            }
        } catch (Exception e) {
            log.warn("캐시 무효화 실패: apiVideoId={}, error={}", apiVideoId, e.getMessage());
        }
    }

    /**
     * 영상 생성 또는 재조회 (동시성 문제 처리)
     */
    private Video createOrRetrieveVideo(String apiVideoId, VideoType actualType) {
        try {
            return processNewVideo(apiVideoId, actualType);

        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // 동시성 문제로 이미 다른 스레드가 저장함 → DB에서 재조회
            log.info("동시성으로 인한 중복 저장 시도 감지: apiVideoId={}, 재조회 수행", apiVideoId);

            return videoRepository.findByApiVideoId(apiVideoId)
                    .orElseThrow(() -> {
                        log.error("DataIntegrityViolationException 후 재조회 실패: apiVideoId={}", apiVideoId);
                        return new BusinessException(VideoError.VIDEO_NOT_FOUND);
                    });
        }
    }

    /**
     * processAndGetSummary 오버로드 (재판별 옵션)
     */
    @Transactional
    public VideoSummaryResponse processAndGetSummary(
            String token, String apiVideoId, JsonNode thumbnails,
            VideoType expectedType, boolean redetectType) {

        // 1. DB 확인
        Optional<Video> existingVideo = videoRepository.findByApiVideoId(apiVideoId);

        if (existingVideo.isPresent()) {
            Video video = existingVideo.get();

            if (expectedType != null) {
                if (video.getVideoType() != null && video.getVideoType() != expectedType) {
                    log.debug("타입 불일치: apiVideoId={}, expected={}, actual={}",
                            apiVideoId, expectedType, video.getVideoType());
                    return null;
                }

                if (video.getVideoType() == null) {
                    // 쓰기 작업 - 별도 트랜잭션
                    return updateVideoTypeAndReturnSummary(token, video, expectedType);
                }
            }

            Long scrapId = getScrapIdSafely(token, apiVideoId);
            return videoMapper.toSummaryResponse(video, scrapId);
        }

        // 2. 신규 영상 - 타입 판별
        VideoType actualType;

        if (!redetectType && expectedType != null) {
            // 재판별 안 함 - expectedType 그대로 사용
            actualType = expectedType;
            log.info("🎯 타입 확정: apiVideoId={}, confirmedType={}", apiVideoId, actualType);
        } else {
            // 기존 로직: VideoTypeDetector로 판별
            actualType = videoTypeDetector.detectVideoType(apiVideoId, thumbnails);
            log.info("🎯 타입 판별: apiVideoId={}, actualType={}, expectedType={}",
                    apiVideoId, actualType, expectedType);
        }

        // expectedType이 null이면 타입 무관하게 처리
        if (expectedType != null && actualType != expectedType) {
            log.info("새 영상 타입 불일치: apiVideoId={}, expected={}, actual={}",
                    apiVideoId, expectedType, actualType);

            // DB에는 저장 (올바른 타입으로) - 별도 트랜잭션
            createOrRetrieveVideoWithTransaction(apiVideoId, actualType);
            return null;
        }

        // 3. 타입 일치 또는 expectedType이 null → 처리 후 반환
        return createNewVideoAndReturnSummaryWithType(token, apiVideoId, actualType);
    }

    /**
     * 영상 생성 (쓰기 작업 분리)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Video createOrRetrieveVideoWithTransaction(String apiVideoId, VideoType actualType) {
        return createOrRetrieveVideo(apiVideoId, actualType);
    }

    /**
     * 신규 영상 생성 및 응답 반환 (쓰기 작업)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public VideoSummaryResponse createNewVideoAndReturnSummaryWithType(
            String token, String apiVideoId, VideoType actualType) {

        Video finalVideo = createOrRetrieveVideo(apiVideoId, actualType);
        Long scrapId = getScrapIdSafely(token, apiVideoId);
        return videoMapper.toSummaryResponse(finalVideo, scrapId);
    }

    /**
     * 스크랩 ID 안전하게 조회 (에러 시 null 반환)
     */
    private Long getScrapIdSafely(String token, String apiVideoId) {
        try {
            return userDataService.getUserScrapId(token, apiVideoId);

        } catch (BusinessException e) {
            // 비즈니스 로직 상 스크랩 없음 (정상)
            log.debug("스크랩 정보 없음: apiVideoId={}, reason={}",
                    apiVideoId, e.getMessage());
            return null;

        } catch (Exception e) {
            // 예상치 못한 에러 (경고 레벨)
            log.warn("스크랩 ID 조회 중 예외 발생: apiVideoId={}, error={}",
                    apiVideoId, e.getMessage());
            return null;
        }
    }
}