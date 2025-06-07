package com.knu.sosuso.capstone.service;

import com.knu.sosuso.capstone.ai.dto.AIAnalysisRequest;
import com.knu.sosuso.capstone.ai.dto.AIAnalysisResponse;
import com.knu.sosuso.capstone.ai.service.AnalysisService;
import com.knu.sosuso.capstone.domain.Comment;
import com.knu.sosuso.capstone.domain.Video;
import com.knu.sosuso.capstone.dto.response.CommentApiResponse;
import com.knu.sosuso.capstone.dto.response.VideoApiResponse;
import com.knu.sosuso.capstone.dto.response.search.UrlChannelDto;
import com.knu.sosuso.capstone.dto.response.search.UrlSearchResponse;
import com.knu.sosuso.capstone.dto.response.search.UrlVideoDto;
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
    public UrlSearchResponse processVideoToSearchResult(String apiVideoId, boolean enableAIAnalysis) {
        if (apiVideoId == null || apiVideoId.trim().isEmpty()) {
            throw new IllegalArgumentException("비디오 ID는 필수입니다.");
        }

        try {
            log.info("비디오 처리 시작: apiVideoId={}, AI분석={}", apiVideoId, enableAIAnalysis);

            // 3. DB 중복 체크 및 처리
            Optional<Video> existingVideo = videoService.findByApiVideoId(apiVideoId);

            if (existingVideo.isPresent()) {
                log.info("DB에서 기존 데이터 발견: apiVideoId={}", apiVideoId);
                return handleExistingVideo(existingVideo.get(), apiVideoId, enableAIAnalysis);
            } else {
                log.info("새로운 데이터 - YouTube API에서 수집: apiVideoId={}", apiVideoId);
                return handleNewVideo(apiVideoId, enableAIAnalysis);
            }

        } catch (Exception e) {
            log.error("비디오 처리 실패: apiVideoId={}, error={}", apiVideoId, e.getMessage());
            throw e;
        }
    }

    /**
     * 기존 DB 데이터가 있는 경우 처리
     */
    private UrlSearchResponse handleExistingVideo(
            Video existingVideo,
            String apiVideoId,
            boolean enableAIAnalysis) {

        LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);
        boolean isWithinOneDay = existingVideo.getCreatedAt().isAfter(oneDayAgo);

        if (!isWithinOneDay) {
            // 1일 지남 - 기존 데이터 삭제 후 새로 처리
            log.info("1일 지난 데이터, 삭제 후 새로 처리: apiVideoId={}", apiVideoId);
            videoService.deleteExistingData(existingVideo.getId());
            return handleNewVideo(apiVideoId, enableAIAnalysis);
        }

        // 1일 이내 - AI 분석 상태 확인
        boolean isAICompleted = videoService.isAIAnalysisCompleted(existingVideo);

        if (isAICompleted) {
            // AI 분석 완료 - DB 데이터로 응답
            log.info("AI 분석 완료된 DB 데이터로 응답: apiVideoId={}", apiVideoId);
            return responseMappingService.mapFromDbToSearchResult(existingVideo);
        } else if (enableAIAnalysis) {
            // AI 분석 미완료 - AI 분석만 재시도
            log.info("AI 분석 미완료, 재시도 (DB 데이터 + AI 재분석): apiVideoId={}", apiVideoId);
            return retryAIAnalysisOnly(existingVideo, apiVideoId);
        } else {
            // AI 분석 비활성화 - 현재 DB 데이터로 응답
            log.info("AI 분석 비활성화, DB 데이터로 응답: apiVideoId={}", apiVideoId);
            return responseMappingService.mapFromDbToSearchResult(existingVideo);
        }
    }

    /**
     * 새로운 비디오 처리
     */
    private UrlSearchResponse handleNewVideo(String apiVideoId, boolean enableAIAnalysis) {

        log.info("YouTube API에서 비디오 정보 수집 시작: apiVideoId={}", apiVideoId);

        // 1. YouTube API로 영상 정보 가져오기
        VideoApiResponse videoInfo = videoService.getVideoInfo(apiVideoId, null);
        log.info("YouTube API - 비디오 정보 수집 완료: title={}", videoInfo.title());

        // 2. 댓글 정보 가져오기
        List<CommentApiResponse.CommentData> allComments = commentService.fetchAllComments(apiVideoId);
        log.info("YouTube API - 댓글 수집 완료: 댓글 수={}", allComments.size());

        if (allComments.isEmpty()) {
            log.info("댓글이 없음, 비디오 정보만 응답 (YouTube API): apiVideoId={}", apiVideoId);
            return createVideoOnlyResponse(videoInfo);
        }

        // 댓글 수 업데이트
        videoInfo = videoService.updateCommentCount(videoInfo, allComments.size());

        // 3. 댓글 분석 (백엔드)
        CommentApiResponse commentInfo = commentService.processCommentsForClient(allComments);
        log.info("백엔드 댓글 분석 완료: 히스토그램={}, 타임스탬프={}",
                commentInfo.commentHistogram().size(), commentInfo.popularTimestamps().size());

        // 4. DB 저장 (AI 분석 필드는 null)
        Long videoId = videoService.saveVideoAndCommentsWithoutAI(videoInfo, commentInfo);
        log.info("DB 저장 완료: videoId={}", videoId);

        // 5. AI 분석 시도
        AIAnalysisResponse aiAnalysisResponse = null;
        if (enableAIAnalysis) {
            log.info("AI 분석 시작: apiVideoId={}", apiVideoId);
            aiAnalysisResponse = performAIAnalysis(apiVideoId, allComments, videoId);

            if (aiAnalysisResponse != null) {
                // 6. AI 분석 성공 - DB 업데이트
                log.info("AI 분석 완료 및 DB 업데이트: apiVideoId={}", apiVideoId);
                videoService.updateWithAIResults(videoId, aiAnalysisResponse);
                commentService.updateCommentsWithAnalysis(aiAnalysisResponse);
            } else {
                log.warn("AI 분석 실패, 백엔드 분석 데이터만 제공: apiVideoId={}", apiVideoId);
            }
        } else {
            log.info("AI 분석 비활성화, 백엔드 분석 데이터만 제공: apiVideoId={}", apiVideoId);
        }

        log.info("최종 응답 생성 (YouTube API + 백엔드 분석 + AI 분석={}): apiVideoId={}",
                aiAnalysisResponse != null ? "성공" : "실패", apiVideoId);

        // 7. 응답 생성
        return responseMappingService.mapToSearchResult(videoInfo, commentInfo, aiAnalysisResponse);
    }

    /**
     * 댓글이 없는 경우 - 영상 정보만 응답
     */
    private UrlSearchResponse createVideoOnlyResponse(VideoApiResponse videoInfo) {
        UrlVideoDto video = responseMappingService.mapToVideoResponse(videoInfo);
        UrlChannelDto channel = responseMappingService.mapToChannelResponse(videoInfo);

        return new UrlSearchResponse(video, channel, null, List.of());
    }

    /**
     * AI 분석만 재시도 (기존 DB 데이터 있는 경우)
     */
    private UrlSearchResponse retryAIAnalysisOnly(Video existingVideo, String apiVideoId) {
        // 1. 먼저 DB에 댓글이 있는지 확인
        List<Comment> existingComments = commentRepository.findAllByVideoId(existingVideo.getId());

        List<CommentApiResponse.CommentData> allComments;
        if (existingComments.isEmpty()) {
            // 댓글이 없으면 새로 저장
            log.info("DB에 댓글 없음, YouTube API에서 댓글 재수집: apiVideoId={}", apiVideoId);
            allComments = commentService.fetchAllComments(apiVideoId);
            CommentApiResponse commentInfo = commentService.processCommentsForClient(allComments);
            commentService.saveCommentsToDb(commentInfo.allComments(), existingVideo);
            log.info("댓글 DB 저장 완료: 댓글 수={}", allComments.size());
        } else {
            log.info("DB에서 기존 댓글 사용: 댓글 수={}", existingComments.size());
            // AI 분석용으로는 최신 데이터가 필요하므로 다시 수집
            allComments = commentService.fetchAllComments(apiVideoId);
        }

        // 2. AI 분석 시도
        log.info("AI 분석 재시도 시작: apiVideoId={}", apiVideoId);
        AIAnalysisResponse analysisResponse = performAIAnalysis(
                apiVideoId,
                allComments,
                existingVideo.getId()
        );

        if (analysisResponse != null) {
            log.info("AI 분석 재시도 성공 및 DB 업데이트: apiVideoId={}", apiVideoId);
            videoService.updateWithAIResults(existingVideo.getId(), analysisResponse);
            commentService.updateCommentsWithAnalysis(analysisResponse);
        } else {
            log.warn("AI 분석 재시도 실패, 기존 DB 데이터로 응답: apiVideoId={}", apiVideoId);
        }

        // 3. 업데이트된 DB 데이터로 응답
        Video updatedVideo = videoService.findById(existingVideo.getId())
                .orElseThrow(() -> new RuntimeException("업데이트된 비디오를 찾을 수 없습니다"));

        log.info("최종 응답 생성 (DB 데이터 + AI 재분석={}): apiVideoId={}",
                analysisResponse != null ? "성공" : "실패", apiVideoId);

        return responseMappingService.mapFromDbToSearchResult(updatedVideo);
    }

    /**
     * AI 분석 수행
     */
    private AIAnalysisResponse performAIAnalysis(String apiVideoId, List<CommentApiResponse.CommentData> allComments, Long videoId) {
        try {
            // AI 분석용 댓글 추출 (전체 최대 개수 사용)
            Map<String, String> commentsForAI = commentService.extractCommentsForAI(allComments);

            if (!commentsForAI.isEmpty()) {
                log.info("AI 분석 요청 시작: apiVideoId={}, 분석 댓글 수={}",
                        apiVideoId, commentsForAI.size());

                AIAnalysisRequest aiAnalysisRequest = new AIAnalysisRequest(apiVideoId, commentsForAI);
                AIAnalysisResponse aiAnalysisResponse = analysisService.requestAnalysis(aiAnalysisRequest);

                // videoId 설정
                AIAnalysisResponse updatedAiAnalysisResponse = new AIAnalysisResponse(
                        videoId,
                        aiAnalysisResponse.apiVideoId(),
                        aiAnalysisResponse.summation(),
                        aiAnalysisResponse.isWarning(),
                        aiAnalysisResponse.keywords(),
                        aiAnalysisResponse.sentimentComments(),
                        aiAnalysisResponse.languageRatio(),
                        aiAnalysisResponse.sentimentRatio()
                );

                log.info("AI 분석 완료: apiVideoId={}, 요약 길이={}, 경고={}",
                        apiVideoId, aiAnalysisResponse.summation().length(), aiAnalysisResponse.isWarning());

                return updatedAiAnalysisResponse;
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