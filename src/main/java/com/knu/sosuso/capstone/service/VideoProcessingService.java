package com.knu.sosuso.capstone.service;

import com.knu.sosuso.capstone.ai.dto.AIAnalysisRequest;
import com.knu.sosuso.capstone.ai.dto.AIAnalysisResponse;
import com.knu.sosuso.capstone.ai.service.AnalysisService;
import com.knu.sosuso.capstone.domain.Comment;
import com.knu.sosuso.capstone.domain.Video;
import com.knu.sosuso.capstone.dto.response.CommentApiResponse;
import com.knu.sosuso.capstone.dto.response.VideoApiResponse;
import com.knu.sosuso.capstone.dto.response.detail.DetailChannelDto;
import com.knu.sosuso.capstone.dto.response.detail.DetailPageResponse;
import com.knu.sosuso.capstone.dto.response.detail.DetailVideoDto;
import com.knu.sosuso.capstone.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@RequiredArgsConstructor
@Service
public class VideoProcessingService {

    private final VideoService videoService;
    private final CommentService commentService;
    private final AnalysisService analysisService;
    private final ResponseMappingService responseMappingService;
    private final CommentRepository commentRepository;

    /**
     * 비디오 완전 처리 (댓글 수집 + AI 분석 + DB 저장)
     * SearchService와 TrendingService에서 공통으로 사용
     *
     * @param apiVideoId       비디오 ID
     * @param enableAIAnalysis AI 분석 수행 여부
     * @return 처리된 비디오 + 댓글 정보
     */
    public DetailPageResponse processVideoToSearchResult(String token, String apiVideoId, boolean enableAIAnalysis) {
        if (apiVideoId == null || apiVideoId.trim().isEmpty()) {
            throw new IllegalArgumentException("비디오 ID는 필수입니다.");
        }

        try {
            log.info("비디오 처리 시작: apiVideoId={}, AI분석={}", apiVideoId, enableAIAnalysis);

            Optional<Video> existingVideo = videoService.findByApiVideoId(apiVideoId);

            if (existingVideo.isPresent()) {
                log.info("DB에서 기존 데이터 발견: apiVideoId={}", apiVideoId);
                return handleExistingVideo(token, existingVideo.get(), apiVideoId, enableAIAnalysis);
            } else {
                log.info("새로운 데이터 - YouTube API에서 수집: apiVideoId={}", apiVideoId);
                return handleNewVideo(token, apiVideoId, enableAIAnalysis);
            }

        } catch (Exception e) {
            log.error("비디오 처리 실패: apiVideoId={}, error={}", apiVideoId, e.getMessage());
            throw e;
        }
    }

    /**
     * 기존 DB 데이터가 있는 경우 처리
     */
    private DetailPageResponse handleExistingVideo(String token, Video existingVideo, String apiVideoId, boolean enableAIAnalysis) {

        LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);  // 기준점 1일로 설정
        boolean isWithinOneDay = existingVideo.getCreatedAt().isAfter(oneDayAgo);

        if (!isWithinOneDay) {
            // 1일 지남 - 기존 데이터 삭제 후 새로 처리
            log.info("1일 지난 데이터, 삭제 후 새로 처리: apiVideoId={}", apiVideoId);
            videoService.deleteExistingData(existingVideo.getId());
            return handleNewVideo(token, apiVideoId, enableAIAnalysis);
        }

        // 1일 이내 - 댓글 유무 먼저 확인
        boolean hasComments = commentRepository.existsByVideoId(existingVideo.getId());

        if (!hasComments) {
            // 댓글 없음 - YouTube API에서 댓글 수집 시도
            return handleExistingVideoWithoutComments(token, existingVideo, apiVideoId, enableAIAnalysis);

        } else {
            // 댓글 있음 - AI 분석 상태 확인 (기존 로직)
            return handleExistingVideoWithComments(token, existingVideo, apiVideoId, enableAIAnalysis);
        }
    }

    /**
     * 기존 비디오에 댓글이 없는 경우 처리
     */
    private DetailPageResponse handleExistingVideoWithoutComments(String token, Video existingVideo, String apiVideoId, boolean enableAIAnalysis) {
        log.info("기존 데이터에 댓글 없음, 댓글 수집 시도: apiVideoId={}", apiVideoId);
        List<CommentApiResponse.CommentData> allComments = commentService.fetchAllComments(apiVideoId);

        if (allComments.isEmpty()) {
            log.info("댓글 수집 시도 후에도 댓글 없음, 기존 비디오 정보로 응답: apiVideoId={}", apiVideoId);
            return createVideoOnlyResponseFromDb(token, existingVideo);
        } else {
            log.info("새로운 댓글 발견, 분석 및 저장 진행: apiVideoId={}, 댓글수={}", apiVideoId, allComments.size());
            return processCommentsForExistingVideo(token, existingVideo, allComments, enableAIAnalysis);
        }
    }

    /**
     * 기존 비디오에 댓글이 있는 경우 처리
     */
    private DetailPageResponse handleExistingVideoWithComments(String token, Video existingVideo, String apiVideoId, boolean enableAIAnalysis) {
        boolean isAICompleted = videoService.isAIAnalysisCompleted(existingVideo);

        if (isAICompleted) {
            log.info("AI 분석 완료된 DB 데이터로 응답: apiVideoId={}", apiVideoId);
            return responseMappingService.mapFromDbToSearchResult(token, existingVideo);
        } else if (enableAIAnalysis) {
            log.info("AI 분석 미완료, 재시도 (DB 데이터 + AI 재분석): apiVideoId={}", apiVideoId);
            return retryAIAnalysisOnly(token, existingVideo, apiVideoId);
        } else {
            log.info("AI 분석 비활성화, DB 데이터로 응답: apiVideoId={}", apiVideoId);
            return responseMappingService.mapFromDbToSearchResult(token, existingVideo);
        }
    }

    /**
     * 새로운 비디오 처리
     */
    private DetailPageResponse handleNewVideo(String token, String apiVideoId, boolean enableAIAnalysis) {

        log.info("YouTube API에서 비디오 정보 수집 시작: apiVideoId={}", apiVideoId);

        // 1. YouTube API로 영상 정보 가져오기
        VideoApiResponse videoInfo = videoService.getVideoInfo(apiVideoId, null);
        log.info("YouTube API - 비디오 정보 수집 완료: title={}", videoInfo.title());

        // 2. 댓글 정보 가져오기
        List<CommentApiResponse.CommentData> allComments = commentService.fetchAllComments(apiVideoId);
        log.info("YouTube API - 댓글 수집 완료: 댓글 수={}", allComments.size());

        if (allComments.isEmpty()) {
            log.info("댓글이 없음, 비디오 정보만 응답 (YouTube API): apiVideoId={}", apiVideoId);
            return createVideoOnlyResponse(token, videoInfo);
        }

        // 댓글 수 업데이트
        videoInfo = videoService.updateCommentCount(videoInfo, allComments.size());
        Long videoId = saveVideoAndCommentsToDb(videoInfo, allComments);

        AIAnalysisResponse aiAnalysisResponse = tryAIAnalysisAndUpdate(apiVideoId, allComments, videoId, enableAIAnalysis);

        log.info("최종 응답 생성 (YouTube API + 백엔드 분석 + AI 분석={}): apiVideoId={}",
                aiAnalysisResponse != null ? "성공" : "실패", apiVideoId);

        CommentApiResponse commentInfo = commentService.processCommentsForClient(allComments);
        return responseMappingService.mapToSearchResult(token, videoInfo, commentInfo, aiAnalysisResponse);
    }

    /**
     * 기존 비디오에 새 댓글 처리
     */
    private DetailPageResponse processCommentsForExistingVideo(String token, Video existingVideo, List<CommentApiResponse.CommentData> allComments, boolean enableAIAnalysis) {
        try {
            processAndSaveCommentsForExistingVideo(existingVideo, allComments);
            AIAnalysisResponse aiAnalysisResponse = tryAIAnalysisAndUpdate(existingVideo.getApiVideoId(), allComments, existingVideo.getId(), enableAIAnalysis);

            Video updatedVideo = videoService.findById(existingVideo.getId())
                    .orElseThrow(() -> new RuntimeException("업데이트된 비디오를 찾을 수 없습니다"));

            log.info("기존 비디오 최종 응답 생성 (새 댓글 + AI 분석={}): apiVideoId={}",
                    aiAnalysisResponse != null ? "성공" : "실패", existingVideo.getApiVideoId());

            return responseMappingService.mapFromDbToSearchResult(token,updatedVideo);

        } catch (Exception e) {
            log.error("기존 비디오 새 댓글 처리 실패: videoId={}, error={}", existingVideo.getId(), e.getMessage());
            throw new RuntimeException("기존 비디오 새 댓글 처리 중 오류 발생", e);
        }
    }

    /**
     * AI 분석만 재시도 (기존 DB 데이터 있는 경우)
     */
    private DetailPageResponse retryAIAnalysisOnly(String token, Video existingVideo, String apiVideoId) {
        List<Comment> existingComments = commentRepository.findAllByVideoId(existingVideo.getId());

        List<CommentApiResponse.CommentData> allComments;
        if (existingComments.isEmpty()) {
            log.info("DB에 댓글 없음, YouTube API에서 댓글 재수집: apiVideoId={}", apiVideoId);
            allComments = commentService.fetchAllComments(apiVideoId);
            processAndSaveCommentsForExistingVideo(existingVideo, allComments);
        } else {
            log.info("DB에서 기존 댓글 사용: 댓글 수={}", existingComments.size());
            allComments = commentService.fetchAllComments(apiVideoId);
        }

        AIAnalysisResponse analysisResponse = tryAIAnalysisAndUpdate(apiVideoId, allComments, existingVideo.getId(), true);

        Video updatedVideo = videoService.findById(existingVideo.getId())
                .orElseThrow(() -> new RuntimeException("업데이트된 비디오를 찾을 수 없습니다"));

        log.info("최종 응답 생성 (DB 데이터 + AI 재분석={}): apiVideoId={}",
                analysisResponse != null ? "성공" : "실패", apiVideoId);

        return responseMappingService.mapFromDbToSearchResult(token, updatedVideo);
    }

    // ===== 공통 로직 메소드들 =====

    /**
     * 새 비디오 + 댓글을 DB에 저장 (공통 로직)
     */
    private Long saveVideoAndCommentsToDb(VideoApiResponse videoInfo, List<CommentApiResponse.CommentData> allComments) {
        CommentApiResponse commentInfo = commentService.processCommentsForClient(allComments);
        log.info("백엔드 댓글 분석 완료: 히스토그램={}, 타임스탬프={}",
                commentInfo.commentHistogram().size(), commentInfo.popularTimestamps().size());

        Long videoId = videoService.saveVideoAndCommentsWithoutAI(videoInfo, commentInfo);
        log.info("DB 저장 완료: videoId={}", videoId);

        return videoId;
    }

    /**
     * 기존 비디오에 댓글 분석 및 저장 (공통 로직)
     */
    private void processAndSaveCommentsForExistingVideo(Video existingVideo, List<CommentApiResponse.CommentData> allComments) {
        CommentApiResponse commentInfo = commentService.processCommentsForClient(allComments);
        log.info("기존 비디오 댓글 분석 완료: 히스토그램={}, 타임스탬프={}",
                commentInfo.commentHistogram().size(), commentInfo.popularTimestamps().size());

        commentService.saveCommentsToDb(commentInfo.allComments(), existingVideo);
        videoService.updateVideoWithCommentAnalysis(existingVideo.getId(), commentInfo);
        log.info("기존 비디오에 댓글 저장 완료: videoId={}, 댓글수={}", existingVideo.getId(), allComments.size());
    }

    /**
     * AI 분석 시도 및 DB 업데이트 (공통 로직)
     */
    private AIAnalysisResponse tryAIAnalysisAndUpdate(String apiVideoId, List<CommentApiResponse.CommentData> allComments, Long videoId, boolean enableAIAnalysis) {
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

    /**
     * AI 분석 수행
     */
    private AIAnalysisResponse performAIAnalysis(String apiVideoId, List<CommentApiResponse.CommentData> allComments, Long videoId) {
        try {
            // AI 분석용 댓글 추출 (전체 최대 개수 사용)
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
}