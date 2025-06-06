package com.knu.sosuso.capstone.service;

import com.knu.sosuso.capstone.ai.dto.AnalysisRequest;
import com.knu.sosuso.capstone.ai.dto.AnalysisResponse;
import com.knu.sosuso.capstone.ai.service.AnalysisService;
import com.knu.sosuso.capstone.dto.response.CommentApiResponse;
import com.knu.sosuso.capstone.dto.response.SearchUrlResponse;
import com.knu.sosuso.capstone.dto.response.VideoApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Service
public class VideoProcessingService {

    private final VideoService videoService;
    private final CommentService commentService;
    private final AnalysisService analysisService;

    /**
     * 비디오 완전 처리 (댓글 수집 + AI 분석 + DB 저장)
     * SearchService와 TrendingService에서 공통으로 사용
     *
     * @param apiVideoId       비디오 ID
     * @param enableAIAnalysis AI 분석 수행 여부
     * @return 처리된 비디오 + 댓글 정보
     */
    public SearchUrlResponse processVideo(String apiVideoId, boolean enableAIAnalysis) {
        if (apiVideoId == null || apiVideoId.trim().isEmpty()) {
            throw new IllegalArgumentException("비디오 ID는 필수입니다.");
        }

        try {
            log.info("비디오 처리 시작: apiVideoId={}, AI분석={}", apiVideoId, enableAIAnalysis);

            // 1. 댓글 정보 가져오기 (한 번의 API 호출로 원본 + 처리된 댓글 모두 획득)
            List<CommentApiResponse.CommentData> allComments = commentService.fetchAllComments(apiVideoId);

            if (allComments.isEmpty()) {
                log.info("댓글이 없음, AI 분석 건너뛰기: apiVideoId={}", apiVideoId);

                CommentApiResponse emptyResponse = new CommentApiResponse(new HashMap<>(), new HashMap<>(), new ArrayList<>());
                VideoApiResponse videoApiResponse = videoService.getVideoInfo(apiVideoId, 0);
                VideoApiResponse saveVideo = videoService.saveVideoInformation(videoApiResponse);
                return new SearchUrlResponse(saveVideo, emptyResponse);
            }

            int totalCommentCount = allComments.size();

            // 2. AI 분석 (이미 가져온 댓글에서 상위 500개만 추출)
            boolean aiAnalysisSuccess = false;
            AnalysisResponse analysisResponse = null;

            if (enableAIAnalysis) {
                try {
                    // AI 분석용 댓글 추출
                    Map<String, String> commentsForAI = commentService.extractCommentsForAI(allComments);

                    if (!commentsForAI.isEmpty()) {
                        log.info("AI 분석 요청 시작: apiVideoId={}, 분석 댓글 수={}",
                                apiVideoId, commentsForAI.size());

                        // FastAPI 서버로 AI 분석 요청
                        AnalysisRequest analysisRequest = new AnalysisRequest(apiVideoId, commentsForAI);
                        analysisResponse = analysisService.requestAnalysis(analysisRequest);
                        aiAnalysisSuccess = true;

                        log.info("AI 분석 성공: apiVideoId={}, 요약 길이={}, 경고={}",
                                apiVideoId, analysisResponse.summation().length(), analysisResponse.isWarning());
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
            } else {
                log.info("AI 분석 비활성화: apiVideoId={}", apiVideoId);
            }

            // 3. 클라이언트용 응답 생성
            CommentApiResponse commentInfo = commentService.processCommentsForClient(allComments);

            Map<Integer, Integer> hourlyDistribution = commentInfo.hourlyDistribution();
            Map<String, Integer> mentionedTimestamp = commentInfo.mentionedTimestamp();

            // 4. 비디오 정보 조회
            VideoApiResponse videoInfo = videoService.getVideoInfo(apiVideoId, totalCommentCount);

            // 5. DB 저장 - 케이스 구분
            if (aiAnalysisSuccess && analysisResponse != null) {
                // AI 분석 성공: 모든 분석 데이터 저장
                commentService.saveComments(commentInfo, analysisResponse);  // 댓글 + 감정
                videoInfo = videoService.saveVideoInformation(videoInfo, hourlyDistribution, mentionedTimestamp, analysisResponse);
                log.info("비디오 저장 완료 (AI 분석 포함): apiVideoId={}", apiVideoId);
            } else {
                // AI 분석 실패
                videoInfo = videoService.saveVideoInformation(videoInfo, hourlyDistribution, mentionedTimestamp);
                log.info("비디오 저장 완료 (AI 분석 미포함): apiVideoId={}", apiVideoId);
            }

            log.info("비디오 처리 완료: apiVideoId={}, 댓글수={}", apiVideoId, totalCommentCount);
            return new SearchUrlResponse(videoInfo, commentInfo);

        } catch (Exception e) {
            log.error("비디오 처리 실패: apiVideoId={}, error={}", apiVideoId, e.getMessage());
            throw e;
        }
    }
}