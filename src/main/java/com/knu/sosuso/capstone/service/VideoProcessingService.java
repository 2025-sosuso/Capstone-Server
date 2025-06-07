package com.knu.sosuso.capstone.service;

import com.knu.sosuso.capstone.ai.dto.AIAnalysisRequest;
import com.knu.sosuso.capstone.ai.dto.AIAnalysisResponse;
import com.knu.sosuso.capstone.ai.service.AnalysisService;
import com.knu.sosuso.capstone.domain.Comment;
import com.knu.sosuso.capstone.domain.Video;
import com.knu.sosuso.capstone.dto.response.CommentApiResponse;
import com.knu.sosuso.capstone.dto.response.VideoApiResponse;
import com.knu.sosuso.capstone.dto.response.search.ChannelResponse;
import com.knu.sosuso.capstone.dto.response.search.SearchResultResponse;
import com.knu.sosuso.capstone.dto.response.search.VideoResponse;
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
    public SearchResultResponse processVideoToSearchResult(String apiVideoId, boolean enableAIAnalysis) {
        if (apiVideoId == null || apiVideoId.trim().isEmpty()) {
            throw new IllegalArgumentException("비디오 ID는 필수입니다.");
        }

        try {
            log.info("비디오 처리 시작: apiVideoId={}, AI분석={}", apiVideoId, enableAIAnalysis);

            // 1. YouTube API로 영상 정보 가져오기
            VideoApiResponse videoInfo = videoService.getVideoInfo(apiVideoId, null);

            // 2. 댓글 정보 가져오기
            List<CommentApiResponse.CommentData> allComments = commentService.fetchAllComments(apiVideoId);

            if (allComments.isEmpty()) {
                log.info("댓글이 없음, 영상 정보만 응답: apiVideoId={}", apiVideoId);
                return createVideoOnlyResponse(videoInfo);
            }

            // 댓글 수 업데이트
            videoInfo = videoService.updateCommentCount(videoInfo, allComments.size());

            // 3. DB 중복 체크 및 처리
            Optional<Video> existingVideo = videoService.findByApiVideoId(apiVideoId);

            if (existingVideo.isPresent()) {
                return handleExistingVideo(existingVideo.get(), videoInfo, allComments, enableAIAnalysis);
            } else {
                return handleNewVideo(videoInfo, allComments, enableAIAnalysis);
            }

        } catch (Exception e) {
            log.error("비디오 처리 실패: apiVideoId={}, error={}", apiVideoId, e.getMessage());
            throw e;
        }
    }

    /**
     * 댓글이 없는 경우 - 영상 정보만 응답
     */
    private SearchResultResponse createVideoOnlyResponse(VideoApiResponse videoInfo) {
        VideoResponse video = responseMappingService.mapToVideoResponse(videoInfo);
        ChannelResponse channel = responseMappingService.mapToChannelResponse(videoInfo);

        return new SearchResultResponse(video, channel, null, List.of());
    }

    /**
     * 기존 DB 데이터가 있는 경우 처리
     */
    private SearchResultResponse handleExistingVideo(
            Video existingVideo,
            VideoApiResponse videoInfo,
            List<CommentApiResponse.CommentData> allComments,
            boolean enableAIAnalysis) {

        LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);
        boolean isWithinOneDay = existingVideo.getCreatedAt().isAfter(oneDayAgo);

        if (!isWithinOneDay) {
            // 1일 지남 - 기존 데이터 삭제 후 새로 처리
            log.info("1일 지난 데이터, 삭제 후 새로 처리: apiVideoId={}", videoInfo.apiVideoId());
            videoService.deleteExistingData(existingVideo.getId());
            return handleNewVideo(videoInfo, allComments, enableAIAnalysis);
        }

        // 1일 이내 - AI 분석 상태 확인
        boolean isAICompleted = videoService.isAIAnalysisCompleted(existingVideo);

        if (isAICompleted) {
            // AI 분석 완료 - DB 데이터로 응답
            log.info("AI 분석 완료된 데이터 존재, DB에서 응답: apiVideoId={}", videoInfo.apiVideoId());
            return responseMappingService.mapFromDbToSearchResult(existingVideo);
        } else if (enableAIAnalysis) {
            // AI 분석 미완료 - AI 분석만 재시도
            log.info("AI 분석 미완료, 재시도: apiVideoId={}", videoInfo.apiVideoId());
            return retryAIAnalysisOnly(existingVideo, allComments);
        } else {
            // AI 분석 비활성화 - 현재 DB 데이터로 응답
            log.info("AI 분석 비활성화, 현재 데이터로 응답: apiVideoId={}", videoInfo.apiVideoId());
            return responseMappingService.mapFromDbToSearchResult(existingVideo);
        }
    }

    /**
     * 새로운 비디오 처리
     */
    private SearchResultResponse handleNewVideo(
            VideoApiResponse videoInfo,
            List<CommentApiResponse.CommentData> allComments,
            boolean enableAIAnalysis) {

        log.info("새로운 비디오 처리: apiVideoId={}", videoInfo.apiVideoId());

        // 1. 댓글 분석 (백엔드)
        CommentApiResponse commentInfo = commentService.processCommentsForClient(allComments);

        // 2. DB 저장 (AI 분석 필드는 null)
        Long videoId = videoService.saveVideoAndCommentsWithoutAI(videoInfo, commentInfo);

        // 3. AI 분석 시도
        AIAnalysisResponse aiAnalysisResponse = null;
        if (enableAIAnalysis) {
            aiAnalysisResponse = performAIAnalysis(videoInfo.apiVideoId(), allComments, videoId);

            if (aiAnalysisResponse != null) {
                // 4. AI 분석 성공 - DB 업데이트
                videoService.updateWithAIResults(videoId, aiAnalysisResponse);
                commentService.updateCommentsWithAnalysis(aiAnalysisResponse);
            }
        }

        log.info("최종 응답 생성 전: 댓글 수={}", commentInfo.allComments().size());

        // 5. 응답 생성
        return responseMappingService.mapToSearchResult(videoInfo, commentInfo, aiAnalysisResponse);
    }

    /**
     * AI 분석만 재시도 (기존 DB 데이터 있는 경우)
     */
    private SearchResultResponse retryAIAnalysisOnly(Video existingVideo, List<CommentApiResponse.CommentData> allComments) {
        // 1. 먼저 DB에 댓글이 있는지 확인
        List<Comment> existingComments = commentRepository.findAllByVideoId(existingVideo.getId());

        if (existingComments.isEmpty()) {
            // 댓글이 없으면 새로 저장
            CommentApiResponse commentInfo = commentService.processCommentsForClient(allComments);
            commentService.saveCommentsToDb(commentInfo.allComments(), existingVideo);
            log.info("기존 비디오에 댓글 추가 저장: videoId={}, 댓글수={}", existingVideo.getId(), allComments.size());
        }

        // 2. AI 분석 시도
        AIAnalysisResponse analysisResponse = performAIAnalysis(
                existingVideo.getApiVideoId(),
                allComments,
                existingVideo.getId()
        );

        if (analysisResponse != null) {
            videoService.updateWithAIResults(existingVideo.getId(), analysisResponse);
            commentService.updateCommentsWithAnalysis(analysisResponse);
        }

        // 3. 업데이트된 DB 데이터로 응답
        Video updatedVideo = videoService.findById(existingVideo.getId())
                .orElseThrow(() -> new RuntimeException("업데이트된 비디오를 찾을 수 없습니다"));

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