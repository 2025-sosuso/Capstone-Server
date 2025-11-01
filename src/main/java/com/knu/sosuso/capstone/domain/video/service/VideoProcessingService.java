package com.knu.sosuso.capstone.domain.video.service;

import com.knu.sosuso.capstone.domain.ai.dto.AIAnalysisRequest;
import com.knu.sosuso.capstone.domain.ai.dto.AIAnalysisResponse;
import com.knu.sosuso.capstone.domain.ai.service.AnalysisService;
import com.knu.sosuso.capstone.domain.conmment.entity.Comment;
import com.knu.sosuso.capstone.domain.conmment.service.CommentService;
import com.knu.sosuso.capstone.domain.conmment.dto.response.CommentApiResponse;
import com.knu.sosuso.capstone.domain.detail.dto.DetailChannelDto;
import com.knu.sosuso.capstone.domain.detail.dto.DetailPageResponse;
import com.knu.sosuso.capstone.domain.detail.dto.DetailVideoDto;
import com.knu.sosuso.capstone.domain.conmment.repository.CommentRepository;
import com.knu.sosuso.capstone.domain.video.entity.Video;
import com.knu.sosuso.capstone.domain.video.dto.response.VideoApiResponse;
import com.knu.sosuso.capstone.domain.video.repository.VideoRepository;
import com.knu.sosuso.capstone.global.config.AppConfig;
import com.knu.sosuso.capstone.global.exception.BusinessException;
import com.knu.sosuso.capstone.global.exception.error.VideoError;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final AppConfig appConfig;

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

        // ========== 4. AI 완료 여부 체크 ==========
        boolean isAICompleted = videoService.isAIAnalysisCompleted(existingVideo);

        if (isAICompleted) {
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

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lastAttempt = existingVideo.getLastAiAttemptAt();

        // 재시도 조건 체크
        boolean shouldRetry = shouldRetryAI(existingVideo, now, lastAttempt);

        if (shouldRetry) {
            log.info("AI 재시도 조건 충족, 백그라운드 처리 시작: apiVideoId={}", apiVideoId);

            // 즉시 응답 (DB 데이터)
            DetailPageResponse response = responseMappingService.mapFromDbToSearchResult(token, existingVideo);

            // 백그라운드에서 AI 재시도
            scheduleBackgroundAIProcessing(existingVideo.getId(), apiVideoId);

            return response;

        } else {
            log.info("AI 재시도 쿨타임 중 ({}분 이내), DB 데이터로 응답: apiVideoId={}",
                    appConfig.getAiRetryCooldownMinutes(), apiVideoId);
            return responseMappingService.mapFromDbToSearchResult(token, existingVideo);
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
        if (video.getLastMetadataUpdatedAt() == null) {
            return video.getCreatedAt().isBefore(updateThreshold);
        }

        return video.getLastMetadataUpdatedAt().isBefore(updateThreshold);
    }

    /**
     * AI 재시도 여부 판단
     */
    private boolean shouldRetryAI(Video video, LocalDateTime now, LocalDateTime lastAttempt) {
        if (video.isAiProcessing()) {
            log.info("AI 처리 중: apiVideoId={}", video.getApiVideoId());
            return false;
        }

        if (lastAttempt == null) {
            log.info("첫 AI 시도: apiVideoId={}", video.getApiVideoId());
            return true;
        }

        LocalDateTime cooldownExpiry = lastAttempt
                .plusMinutes(appConfig.getAiRetryCooldownMinutes());

        boolean canRetry = now.isAfter(cooldownExpiry);

        if (canRetry) {
            log.info("AI 재시도 가능 (쿨타임 경과): apiVideoId={}, 마지막시도={}, 현재시간={}, 재시도횟수={}",
                    video.getApiVideoId(), lastAttempt, now, video.getAiRetryCount());
        } else {
            log.debug("AI 재시도 쿨타임 중: apiVideoId={}, 남은시간={}분",
                    video.getApiVideoId(),
                    java.time.temporal.ChronoUnit.MINUTES.between(now, cooldownExpiry));
        }

        return canRetry;
    }

    /**
     * 백그라운드 메타데이터 업데이트
     */
    @Async("videoProcessingExecutor")
    @Transactional
    public void scheduleMetadataUpdate(Long videoId, String apiVideoId) {
        try {
            log.info("백그라운드 메타데이터 업데이트 시작: videoId={}", videoId);

            videoService.updateMetadataOnly(videoId, apiVideoId);

            log.info("백그라운드 메타데이터 업데이트 완료: videoId={}", videoId);

        } catch (Exception e) {
            log.error("백그라운드 메타데이터 업데이트 실패: videoId={}, error={}",
                    videoId, e.getMessage(), e);
        }
    }

    /**
     * 백그라운드 AI 처리 스케줄링
     */
    @Async("videoProcessingExecutor")
    @Transactional
    public void scheduleBackgroundAIProcessing(Long videoId, String apiVideoId) {
        try {
            log.info("백그라운드 AI 처리 시작: videoId={}", videoId);

            // 처리 중 플래그 세팅
            Video video = videoRepository.findById(videoId)
                    .orElseThrow(() -> new BusinessException(VideoError.VIDEO_NOT_FOUND));

            video.setAiProcessing(true);
            video.setLastAiAttemptAt(LocalDateTime.now());
            videoRepository.save(video);

            // 댓글 조회 (DB 우선)
            List<Comment> existingComments = commentRepository.findAllByVideoId(videoId);

            List<CommentApiResponse.CommentData> commentDataList;
            if (existingComments.isEmpty()) {
                log.info("DB에 댓글 없음, YouTube API에서 수집: apiVideoId={}", apiVideoId);
                commentDataList = commentService.fetchAllComments(apiVideoId);

                if (commentDataList.isEmpty()) {
                    video.setHasNoComments(true);
                    video.setAiProcessing(false);
                    videoRepository.save(video);
                    log.info("댓글 수집 결과 없음, 플래그 업데이트: apiVideoId={}", apiVideoId);
                    return;
                }

                commentService.saveCommentsToDb(commentDataList, video);
            } else {
                log.info("DB에서 기존 댓글 사용: 댓글 수={}", existingComments.size());
                commentDataList = existingComments.stream()
                        .map(c -> new CommentApiResponse.CommentData(
                                c.getApiCommentId(), c.getWriter(), c.getCommentContent(),
                                c.getLikeCount(), null, c.getWrittenAt()
                        ))
                        .collect(Collectors.toList());
            }

            // AI 분석 실행
            AIAnalysisResponse aiResponse = performAIAnalysis(apiVideoId, commentDataList, videoId);

            if (aiResponse != null) {
                videoService.updateWithAIResults(videoId, aiResponse);
                commentService.updateCommentsWithAnalysis(aiResponse);

                video.setAiProcessing(false);
                video.setAiRetryCount(0); // 성공 시 카운트 리셋
                videoRepository.save(video);

                log.info("백그라운드 AI 처리 완료: videoId={}", videoId);
            } else {
                // AI 실패 - 재시도 카운트만 증가 (횟수 제한 없음)
                video.setAiProcessing(false);
                video.setAiRetryCount(video.getAiRetryCount() + 1);
                video.setLastAiAttemptAt(LocalDateTime.now()); // 현재 시간 기록
                videoRepository.save(video);

                log.warn("백그라운드 AI 처리 실패: videoId={}, retryCount={} (5분 후 재시도 가능)",
                        videoId, video.getAiRetryCount());
            }

        } catch (Exception e) {
            log.error("백그라운드 AI 처리 중 오류: videoId={}, error={}", videoId, e.getMessage(), e);

            // 오류 시 플래그 해제 및 재시도 준비
            videoRepository.findById(videoId).ifPresent(v -> {
                v.setAiProcessing(false);
                v.setAiRetryCount(v.getAiRetryCount() + 1);
                v.setLastAiAttemptAt(LocalDateTime.now()); // 현재 시간 기록
                videoRepository.save(v);

                log.warn("재시도 준비: videoId={}, 재시도 횟수={}, 다음 재시도 가능시간={}",
                        v.getId(), v.getAiRetryCount(),
                        v.getLastAiAttemptAt().plusMinutes(5));
            });
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
     * AI 분석 시도 및 DB 업데이트
     */
    @Transactional
    public AIAnalysisResponse tryAIAnalysisAndUpdate(String apiVideoId,
                                                     List<CommentApiResponse.CommentData> allComments,
                                                     Long videoId, boolean enableAIAnalysis) {
        if (!enableAIAnalysis) {
            log.info("AI 분석 비활성화, 백엔드 분석 데이터만 제공: apiVideoId={}", apiVideoId);
            return null;
        }

        log.info("AI 분석 시작: apiVideoId={}", apiVideoId);
        AIAnalysisResponse aiAnalysisResponse = performAIAnalysis(apiVideoId, allComments, videoId);

        if (aiAnalysisResponse != null) {
            log.info("AI 분석 완료 및 DB 업데이트: apiVideoId={}", apiVideoId);
            videoService.updateWithAIResults(videoId, aiAnalysisResponse);
            commentService.updateCommentsWithAnalysis(aiAnalysisResponse);
        } else {
            log.warn("AI 분석 실패, 백엔드 분석 데이터만 제공: apiVideoId={}", apiVideoId);
        }

        return aiAnalysisResponse;
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
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            log.error("AI 서버 클라이언트 오류: apiVideoId={}, status={}", apiVideoId, e.getStatusCode());
        } catch (org.springframework.web.client.HttpServerErrorException e) {
            log.error("AI 서버 내부 오류: apiVideoId={}, status={}", apiVideoId, e.getStatusCode());
        } catch (RuntimeException e) {
            log.error("AI 분석 실패: apiVideoId={}, error={}", apiVideoId, e.getMessage());
        } catch (Exception e) {
            log.error("AI 분석 예상치 못한 오류: apiVideoId={}, error={}", apiVideoId, e.getMessage());
        }

        return null;
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
                .hasNoComments(true)  // 댓글 없음 표시
                .commentsDisabled(false)
                .build();

        videoRepository.save(video);
        log.info("댓글 없는 영상 DB 저장: apiVideoId={}", videoInfo.apiVideoId());
    }
}