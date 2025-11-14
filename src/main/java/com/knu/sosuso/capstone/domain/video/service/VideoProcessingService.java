package com.knu.sosuso.capstone.domain.video.service;

import com.knu.sosuso.capstone.domain.ai.dto.AIAnalysisRequest;
import com.knu.sosuso.capstone.domain.ai.dto.AIAnalysisResponse;
import com.knu.sosuso.capstone.domain.ai.service.AnalysisService;
import com.knu.sosuso.capstone.domain.comment.entity.Comment;
import com.knu.sosuso.capstone.domain.comment.service.CommentService;
import com.knu.sosuso.capstone.domain.comment.dto.response.CommentApiResponse;
import com.knu.sosuso.capstone.domain.detail.dto.DetailChannelDto;
import com.knu.sosuso.capstone.domain.detail.dto.DetailPageResponse;
import com.knu.sosuso.capstone.domain.detail.dto.DetailVideoDto;
import com.knu.sosuso.capstone.domain.comment.repository.CommentRepository;
import com.knu.sosuso.capstone.domain.video.entity.Video;
import com.knu.sosuso.capstone.domain.video.entity.VideoStatusHelper;
import com.knu.sosuso.capstone.domain.video.entity.value.AIAnalysisStatus;
import com.knu.sosuso.capstone.domain.video.dto.response.VideoApiResponse;
import com.knu.sosuso.capstone.domain.video.repository.VideoRepository;
import com.knu.sosuso.capstone.global.config.AppConfig;
import com.knu.sosuso.capstone.global.exception.BusinessException;
import com.knu.sosuso.capstone.global.exception.error.VideoError;
import com.knu.sosuso.capstone.global.service.mapper.ResponseMappingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class VideoProcessingService {

    private final VideoService videoService;
    private final CommentService commentService;
    private final AnalysisService analysisService;
    private final ResponseMappingService responseMappingService;
    private final CommentRepository commentRepository;
    private final VideoRepository videoRepository;
    private final CacheManager cacheManager;
    private final AppConfig appConfig;

    private static final int MAX_AI_RETRY_COUNT = 3;
    private static final long RETRY_DELAY_MS = 2000L;

    /**
     * 메인 진입점: 비디오 처리 (Fast Path 적용)
     */
    @Transactional
    public DetailPageResponse processVideoToSearchResult(String token, String apiVideoId,
                                                         boolean enableAIAnalysis) {
        if (apiVideoId == null || apiVideoId.trim().isEmpty()) {
            throw new BusinessException(VideoError.VIDEO_ID_REQUIRED);
        }

        try {
            log.info("비디오 처리 시작: apiVideoId={}, AI분석={}", apiVideoId, enableAIAnalysis);

            Optional<Video> existingVideo = videoService.findByApiVideoId(apiVideoId);

            if (existingVideo.isPresent()) {
                return handleExistingVideoFastPath(token, existingVideo.get(), apiVideoId, enableAIAnalysis);
            } else {
                return handleNewVideo(token, apiVideoId, enableAIAnalysis);
            }

        } catch (Exception e) {
            log.error("비디오 처리 실패: apiVideoId={}, error={}", apiVideoId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Fast Path: DB 데이터 우선 반환
     */
    @Transactional
    public DetailPageResponse handleExistingVideoFastPath(String token, Video existingVideo,
                                                          String apiVideoId, boolean enableAIAnalysis) {

        // ========== 1. 삭제 확인 ==========
        if (shouldCheckDeletion(existingVideo)) {
            boolean isDeleted = videoService.checkIfVideoDeleted(apiVideoId);

            if (isDeleted) {
                existingVideo.setDeleted(true);
                existingVideo.setDeleteCheckedAt(LocalDateTime.now());
                videoRepository.save(existingVideo);

                throw new BusinessException(VideoError.VIDEO_DELETED);
            }

            existingVideo.setDeleteCheckedAt(LocalDateTime.now());
            videoRepository.save(existingVideo);
        }

        // ========== 2. 메타데이터 갱신 ==========
        LocalDateTime updateThreshold = LocalDateTime.now()
                .minusDays(appConfig.getMetadataUpdateDays());

        if (shouldUpdateMetadata(existingVideo, updateThreshold)) {
            log.info("메타데이터 갱신 시작: apiVideoId={}", apiVideoId);
            scheduleMetadataUpdate(existingVideo.getId(), apiVideoId);
        }

        // ========== 3. 댓글 없는 영상 체크 ==========
        if (existingVideo.isCommentsDisabled() || existingVideo.isHasNoComments()) {
            log.info("댓글 없는 영상, Fast Path 응답: apiVideoId={}", apiVideoId);
            return createVideoOnlyResponseFromDb(token, existingVideo);
        }

        // ========== 4. AI 완료 여부 체크 (AIAnalysisStatus 활용) ==========
        if (existingVideo.getAiAnalysisStatus() == AIAnalysisStatus.COMPLETED) {
            log.info("Fast Path: AI 완료, DB 조회만 (즉시 응답): apiVideoId={}", apiVideoId);
            return responseMappingService.mapFromDbToSearchResult(token, existingVideo);
        }

        // ========== 5. AI 미완료 처리 ==========
        if (enableAIAnalysis) {
            return handleAIIncompleteFastPath(token, existingVideo, apiVideoId);
        } else {
            log.info("AI 비활성화, DB 데이터로 응답: apiVideoId={}", apiVideoId);
            return responseMappingService.mapFromDbToSearchResult(token, existingVideo);
        }
    }

    /**
     * AI 미완료 시 Fast Path 처리
     */
    @Transactional
    public DetailPageResponse handleAIIncompleteFastPath(String token, Video existingVideo,
                                                         String apiVideoId) {

        // AIAnalysisStatus 기반으로 재시도 가능 여부 확인
        if (!VideoStatusHelper.needsAIAnalysis(existingVideo)) {
            log.info("AI 분석 불필요 또는 처리 중: apiVideoId={}, status={}",
                    apiVideoId, existingVideo.getAiAnalysisStatus());
            return responseMappingService.mapFromDbToSearchResult(token, existingVideo);
        }

        // 재시도 가능한지 체크
        if (VideoStatusHelper.canRetry(existingVideo, MAX_AI_RETRY_COUNT)) {
            log.info("AI 재시도 시작: apiVideoId={}, retryCount={}/{}",
                    apiVideoId, existingVideo.getAiRetryCount(), MAX_AI_RETRY_COUNT);

            // 즉시 응답 (DB 데이터)
            DetailPageResponse response = responseMappingService.mapFromDbToSearchResult(token, existingVideo);

            // 백그라운드에서 AI 재시도
            scheduleBackgroundAIProcessingWithRetry(existingVideo.getId(), apiVideoId);

            return response;

        } else {
            log.info("AI 재시도 최대 횟수 도달: apiVideoId={}, retryCount={}",
                    apiVideoId, existingVideo.getAiRetryCount());
            return responseMappingService.mapFromDbToSearchResult(token, existingVideo);
        }
    }

    /**
     * 백그라운드 AI 처리 - 기존 메서드 (호환성 유지)
     * 새로운 @Retryable 메서드를 호출
     */
    @Async("videoProcessingExecutor")
    public void scheduleBackgroundAIProcessing(Long videoId, String apiVideoId) {
        log.info("[비동기] 백그라운드 AI 처리 시작 (기존 메서드): videoId={}, apiVideoId={}", videoId, apiVideoId);
        // @Retryable이 적용된 새 메서드 호출
        scheduleBackgroundAIProcessingWithRetry(videoId, apiVideoId);
    }

    /**
     * 백그라운드 AI 처리 - @Retryable 적용
     */
    @Async("videoProcessingExecutor")
    @Retryable(
            value = {RuntimeException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = RETRY_DELAY_MS, multiplier = 2)
    )
    public void scheduleBackgroundAIProcessingWithRetry(Long videoId, String apiVideoId) {
        log.info("[비동기] 백그라운드 AI 처리 시작: videoId={}, apiVideoId={}", videoId, apiVideoId);

        try {
            Video video = videoRepository.findById(videoId)
                    .orElseThrow(() -> new RuntimeException("Video not found: " + videoId));

            // 동시 실행 방지
            if (video.getAiAnalysisStatus().isProcessing()) {
                log.warn("이미 AI 처리 중: apiVideoId={}", apiVideoId);
                return;
            }

            // AI 분석 시작 상태로 변경
            VideoStatusHelper.startAIAnalysis(video);
            videoRepository.save(video);

            // AI 분석 실행
            boolean success = performBackgroundAIAnalysis(video, apiVideoId);

            if (success) {
                VideoStatusHelper.completeAIAnalysis(video);
            } else {
                throw new RuntimeException("AI 분석 실패: " + apiVideoId);
            }

            videoRepository.save(video);
            evictVideoCache(apiVideoId);

        } catch (Exception e) {
            log.error("[비동기] 백그라운드 AI 처리 실패: apiVideoId={}, error={}",
                    apiVideoId, e.getMessage());
            throw new RuntimeException("AI 처리 실패", e);
        }
    }

    /**
     * @Recover 메서드 - 재시도 모두 실패 시 호출
     */
    @Recover
    public void recoverFromAIFailure(RuntimeException e, Long videoId, String apiVideoId) {
        log.error("[Recover] AI 처리 최종 실패: videoId={}, apiVideoId={}, error={}",
                videoId, apiVideoId, e.getMessage());

        try {
            Video video = videoRepository.findById(videoId).orElse(null);
            if (video != null) {
                VideoStatusHelper.failAIAnalysis(video, e.getMessage());
                videoRepository.save(video);
            }
        } catch (Exception ex) {
            log.error("[Recover] 상태 업데이트 실패: {}", ex.getMessage());
        }
    }

    /**
     * 백그라운드 AI 분석 수행 (실제 로직)
     */
    @Transactional
    public boolean performBackgroundAIAnalysis(Video video, String apiVideoId) {
        try {
            // 댓글 조회
            List<Comment> comments = commentRepository.findByVideo(video);

            if (comments.isEmpty()) {
                VideoStatusHelper.skipAIAnalysis(video, "No comments found");
                videoRepository.save(video);
                return true;  // 댓글이 없는 경우는 성공으로 처리
            }

            // AI 분석용 댓글 데이터 준비
            Map<String, String> commentsForAI = comments.stream()
                    .collect(Collectors.toMap(
                            Comment::getApiCommentId,
                            Comment::getCommentContent,
                            (v1, v2) -> v1,
                            LinkedHashMap::new
                    ));

            log.info("AI 분석 요청: apiVideoId={}, 댓글 수={}", apiVideoId, commentsForAI.size());

            // AI 분석 요청
            AIAnalysisRequest request = new AIAnalysisRequest(apiVideoId, commentsForAI);
            AIAnalysisResponse response = analysisService.requestAnalysis(request);

            if (response != null) {
                // DB 업데이트
                videoService.updateWithAIResults(video.getId(), response);
                commentService.updateCommentsWithAnalysis(response);

                log.info("AI 분석 성공: apiVideoId={}", apiVideoId);
                return true;
            } else {
                log.error("AI 분석 응답이 null: apiVideoId={}", apiVideoId);
                return false;
            }

        } catch (Exception e) {
            log.error("백그라운드 AI 분석 실패: apiVideoId={}, error={}",
                    apiVideoId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 삭제 확인이 필요한가?
     */
    private boolean shouldCheckDeletion(Video video) {
        if (video.isDeleted()) {
            return false; // 이미 삭제된 것으로 확인됨
        }

        LocalDateTime checkThreshold = LocalDateTime.now()
                .minusDays(appConfig.getDeletionCheckDays());

        return video.getDeleteCheckedAt() == null ||
                video.getDeleteCheckedAt().isBefore(checkThreshold);
    }

    /**
     * 메타데이터 업데이트가 필요한가?
     */
    private boolean shouldUpdateMetadata(Video video, LocalDateTime updateThreshold) {
        return video.getLastMetadataUpdatedAt() == null ||
                video.getLastMetadataUpdatedAt().isBefore(updateThreshold);
    }

    /**
     * 메타데이터 업데이트 스케줄링 (비동기)
     */
    @Async("videoProcessingExecutor")
    public void scheduleMetadataUpdate(Long videoId, String apiVideoId) {
        log.info("[비동기] 메타데이터 업데이트 시작: videoId={}", videoId);

        try {
            VideoApiResponse updatedInfo = videoService.getVideoInfo(apiVideoId);

            Video video = videoRepository.findById(videoId).orElseThrow();

            // 메타데이터 업데이트
            video.setViewCount(updatedInfo.viewCount());
            video.setLikeCount(updatedInfo.likeCount());
            video.setCommentCount(updatedInfo.commentCount());
            video.setSubscriberCount(updatedInfo.subscriberCount());
            video.setLastMetadataUpdatedAt(LocalDateTime.now());
            video.setMetadataUpdateCount(video.getMetadataUpdateCount() + 1);

            videoRepository.save(video);

            log.info("[비동기] 메타데이터 업데이트 완료: videoId={}, viewCount={}",
                    videoId, updatedInfo.viewCount());

            // 캐시 무효화
            evictVideoCache(apiVideoId);

        } catch (Exception e) {
            log.error("[비동기] 메타데이터 업데이트 실패: videoId={}, error={}",
                    videoId, e.getMessage());
        }
    }

    /**
     * 새로운 비디오 처리
     */
    @Transactional
    public DetailPageResponse handleNewVideo(String token, String apiVideoId, boolean enableAIAnalysis) {
        log.info("YouTube API에서 비디오 정보 수집 시작: apiVideoId={}", apiVideoId);

        VideoApiResponse videoInfo = videoService.getVideoInfo(apiVideoId);
        log.info("YouTube API - 비디오 정보 수집 완료: title={}", videoInfo.title());

        List<CommentApiResponse.CommentData> allComments = commentService.fetchAllComments(apiVideoId);
        log.info("YouTube API - 댓글 수집 완료: 댓글 수={}", allComments.size());

        if (allComments.isEmpty()) {
            log.info("댓글이 없음: apiVideoId={}", apiVideoId);
            saveVideoWithoutComments(videoInfo);
            return createVideoOnlyResponse(token, videoInfo);
        }

        Long videoId = saveVideoAndCommentsToDb(videoInfo, allComments);
        AIAnalysisResponse aiAnalysisResponse = tryAIAnalysisAndUpdate(apiVideoId, allComments, videoId, enableAIAnalysis);

        log.info("최종 응답 생성 (YouTube API + 백엔드 분석 + AI 분석={}): apiVideoId={}",
                aiAnalysisResponse != null ? "성공" : "실패", apiVideoId);

        CommentApiResponse commentInfo = commentService.processCommentsForClient(allComments);
        return responseMappingService.mapToSearchResult(token, videoInfo, commentInfo, aiAnalysisResponse);
    }

    /**
     * 비디오 + 댓글 DB 저장
     */
    @Transactional
    public Long saveVideoAndCommentsToDb(VideoApiResponse videoInfo, List<CommentApiResponse.CommentData> allComments) {
        CommentApiResponse commentInfo = commentService.processCommentsForClient(allComments);
        log.info("백엔드 댓글 분석 완료: 히스토그램={}, 타임스탬프={}",
                commentInfo.commentHistogram().size(), commentInfo.popularTimestamps().size());

        Long videoId = videoService.saveVideoAndCommentsWithoutAI(videoInfo, commentInfo);
        log.info("DB 저장 완료: videoId={}", videoId);
        return videoId;
    }

    /**
     * AI 분석 시도 및 DB 업데이트 - @Retryable 적용
     */
    @Transactional
    @Retryable(
            value = {RuntimeException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 1.5)
    )
    public AIAnalysisResponse tryAIAnalysisAndUpdate(String apiVideoId,
                                                     List<CommentApiResponse.CommentData> allComments,
                                                     Long videoId, boolean enableAIAnalysis) {
        if (!enableAIAnalysis) {
            log.info("AI 분석 비활성화, 백엔드 분석 데이터만 제공: apiVideoId={}", apiVideoId);
            return null;
        }

        log.info("AI 분석 시작: apiVideoId={}", apiVideoId);

        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video not found: " + videoId));

        // AI 분석 시작 상태 설정
        VideoStatusHelper.startAIAnalysis(video);
        videoRepository.save(video);

        try {
            AIAnalysisResponse aiAnalysisResponse = performAIAnalysis(apiVideoId, allComments, videoId);

            if (aiAnalysisResponse != null) {
                log.info("AI 분석 완료 및 DB 업데이트: apiVideoId={}", apiVideoId);
                videoService.updateWithAIResults(videoId, aiAnalysisResponse);
                commentService.updateCommentsWithAnalysis(aiAnalysisResponse);

                // AI 분석 완료 상태 설정
                VideoStatusHelper.completeAIAnalysis(video);
                videoRepository.save(video);

                return aiAnalysisResponse;
            } else {
                throw new RuntimeException("AI 분석 실패: 응답이 null");
            }
        } catch (Exception e) {
            VideoStatusHelper.retryAIAnalysis(video);
            videoRepository.save(video);
            throw new RuntimeException("AI 분석 실패", e);
        }
    }

    /**
     * @Recover - tryAIAnalysisAndUpdate 실패 시
     */
    @Recover
    public AIAnalysisResponse recoverFromAIAnalysisFailure(RuntimeException e, String apiVideoId,
                                                           List<CommentApiResponse.CommentData> allComments,
                                                           Long videoId, boolean enableAIAnalysis) {
        log.error("[Recover] AI 분석 최종 실패: apiVideoId={}, videoId={}", apiVideoId, videoId);

        Video video = videoRepository.findById(videoId).orElse(null);
        if (video != null) {
            VideoStatusHelper.failAIAnalysis(video, "최대 재시도 횟수 초과");
            videoRepository.save(video);
        }

        return null;
    }

    /**
     * AI 분석 수행
     */
    private AIAnalysisResponse performAIAnalysis(String apiVideoId,
                                                 List<CommentApiResponse.CommentData> allComments,
                                                 Long videoId) {
        try {
            Map<String, String> commentsForAI = commentService.extractCommentsForAI(allComments);

            if (!commentsForAI.isEmpty()) {
                log.info("AI 분석 요청 시작: apiVideoId={}, 분석 댓글 수={}", apiVideoId, commentsForAI.size());

                AIAnalysisRequest aiAnalysisRequest = new AIAnalysisRequest(apiVideoId, commentsForAI);
                AIAnalysisResponse aiAnalysisResponse = analysisService.requestAnalysis(aiAnalysisRequest);

                AIAnalysisResponse updatedResponse = new AIAnalysisResponse(
                        videoId, aiAnalysisResponse.apiVideoId(), aiAnalysisResponse.summation(),
                        aiAnalysisResponse.isWarning(), aiAnalysisResponse.keywords(),
                        aiAnalysisResponse.sentimentComments(), aiAnalysisResponse.languageRatio(),
                        aiAnalysisResponse.sentimentRatio()
                );

                log.info("AI 분석 완료: apiVideoId={}, 요약 길이={}, 경고={}",
                        apiVideoId, aiAnalysisResponse.summation().length(), aiAnalysisResponse.isWarning());

                return updatedResponse;
            }
        } catch (org.springframework.web.client.ResourceAccessException e) {
            log.error("AI 서버 연결 실패 (네트워크): apiVideoId={}, error={}", apiVideoId, e.getMessage());
            throw new RuntimeException("AI 서버 연결 실패", e);
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            log.error("AI 서버 클라이언트 오류: apiVideoId={}, status={}", apiVideoId, e.getStatusCode());
            throw new RuntimeException("AI 서버 클라이언트 오류", e);
        } catch (org.springframework.web.client.HttpServerErrorException e) {
            log.error("AI 서버 내부 오류: apiVideoId={}, status={}", apiVideoId, e.getStatusCode());
            throw new RuntimeException("AI 서버 내부 오류", e);
        } catch (Exception e) {
            log.error("AI 분석 예상치 못한 오류: apiVideoId={}, error={}", apiVideoId, e.getMessage());
            throw new RuntimeException("AI 분석 실패", e);
        }

        return null;
    }

    /**
     * 비디오 캐시 무효화
     */
    private void evictVideoCache(String apiVideoId) {
        try {
            Cache cache = cacheManager.getCache("videoDetail");
            if (cache != null) {
                cache.evict("basic-" + apiVideoId);
                cache.evict("analysis-" + apiVideoId);
                cache.evict("ai-" + apiVideoId);
                cache.evict(apiVideoId);  // deprecated API용

                log.info("비디오 캐시 무효화 완료: apiVideoId={}", apiVideoId);
            }
        } catch (Exception e) {
            log.warn("캐시 무효화 실패: apiVideoId={}, error={}", apiVideoId, e.getMessage());
        }
    }

    /**
     * 댓글이 없는 경우 - 영상 정보만 응답 (YouTube API 데이터)
     */
    private DetailPageResponse createVideoOnlyResponse(String token, VideoApiResponse videoInfo) {
        DetailVideoDto video = responseMappingService.mapToVideoResponse(token, videoInfo);
        DetailChannelDto channel = responseMappingService.mapToChannelResponse(token, videoInfo);
        return new DetailPageResponse(video, channel, null, List.of());
    }

    /**
     * 댓글이 없는 경우 - 영상 정보만 응답 (DB 데이터)
     */
    private DetailPageResponse createVideoOnlyResponseFromDb(String token, Video video) {
        VideoApiResponse videoInfo = new VideoApiResponse(
                video.getApiVideoId(), video.getTitle(), video.getDescription(),
                video.getViewCount(), video.getLikeCount(), video.getCommentCount(),
                video.getThumbnailUrl(), video.getChannelId(), video.getChannelName(),
                video.getChannelThumbnailUrl(),
                video.getSubscriberCount(), video.getUploadedAt()
        );

        DetailVideoDto videoDto = responseMappingService.mapToVideoResponse(token, videoInfo);
        DetailChannelDto channelDto = responseMappingService.mapToChannelResponse(token, videoInfo);
        return new DetailPageResponse(videoDto, channelDto, null, List.of());
    }

    // 댓글 없는 영상 DB 저장
    @Transactional
    public void saveVideoWithoutComments(VideoApiResponse videoInfo) {
        Video video = Video.builder()
                .apiVideoId(videoInfo.apiVideoId())
                .title(videoInfo.title())
                .description(videoInfo.description())
                .viewCount(videoInfo.viewCount())
                .likeCount(videoInfo.likeCount())
                .commentCount(videoInfo.commentCount())
                .thumbnailUrl(videoInfo.thumbnailUrl())
                .channelId(videoInfo.channelId())
                .channelName(videoInfo.channelTitle())
                .channelThumbnailUrl(videoInfo.channelThumbnailUrl())
                .subscriberCount(videoInfo.subscriberCount())
                .uploadedAt(videoInfo.publishedAt())
                .hasNoComments(true)
                .commentsDisabled(false)
                .aiAnalysisStatus(AIAnalysisStatus.SKIPPED)  // AI 상태 설정
                .build();

        videoRepository.save(video);
        log.info("댓글 없는 영상 DB 저장: apiVideoId={}, aiStatus=SKIPPED", videoInfo.apiVideoId());
    }
}