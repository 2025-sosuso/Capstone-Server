package com.knu.sosuso.capstone.service;

import com.knu.sosuso.capstone.ai.dto.AnalysisRequest;
import com.knu.sosuso.capstone.ai.dto.AnalysisResponse;
import com.knu.sosuso.capstone.ai.service.AnalysisService;
import com.knu.sosuso.capstone.dto.response.CommentApiResponse;
import com.knu.sosuso.capstone.dto.response.SearchResponse;
import com.knu.sosuso.capstone.dto.response.SearchUrlResponse;
import com.knu.sosuso.capstone.dto.response.VideoApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@RequiredArgsConstructor
@Service
public class SearchService {

    private static final int MAX_AI_ANALYSIS_COMMENTS = 500;

    private final VideoService videoService;
    private final CommentService commentService;
    private final ChannelService channelService;
    private final AnalysisService analysisService;

    // 1. 검색어 입력
    public SearchResponse search(String query) {
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("검색어는 필수입니다");
        }

        String trimmedQuery = query.trim();
        log.info("검색 요청: query={}, type={}", trimmedQuery, isVideoUrl(trimmedQuery) ? "URL" : "CHANNEL");

        // 2. URL or 채널 구분
        // -> URL이면 영상 정보 추출 메서드 호출
        try {
            if (isVideoUrl(trimmedQuery)) {
                SearchUrlResponse videoResult = searchVideo(trimmedQuery);
                return new SearchResponse("URL", videoResult);
            } else {
                var channelResult = searchChannels(trimmedQuery);
                return new SearchResponse("CHANNEL", channelResult);
            }
        } catch (Exception e) {
            log.error("검색 실패: query={}, error={}", trimmedQuery, e.getMessage(), e);
            throw e;
        }
    }

    private boolean isVideoUrl(String url) {
        return url.contains("youtube.com/watch?v=") || url.contains("youtu.be/");
    }

    // 3. 영상 정보 가져옴
    private SearchUrlResponse searchVideo(String videoUrl) {
        String apiVideoId = videoService.extractVideoId(videoUrl);

        if (apiVideoId == null || apiVideoId.trim().isEmpty()) {
            throw new IllegalArgumentException("유효하지 않은 YouTube URL입니다");
        }

        log.info("비디오 검색 시작: apiVideoId={}", apiVideoId);

        try {
            // 관련도순 상한선 기준 전체 댓글 가져오기
            List<CommentApiResponse.CommentData> allComments = commentService.fetchAllComments(apiVideoId);

            if (allComments.isEmpty()) {
                log.info("댓글 불러오기 실패, AI 분석을 건너뜁니다: apiVideoId={}", apiVideoId);

                VideoApiResponse videoApiResponse = videoService.getVideoInfo(apiVideoId, 0);
                videoService.saveVideoInformation(videoApiResponse);
                CommentApiResponse emptyComment = new CommentApiResponse(new HashMap<>(), new HashMap<>(), new ArrayList<>());
                return new SearchUrlResponse(videoApiResponse, emptyComment);
            }

            // AI 분석용: 관련도순 상위 500개
            Map<String, String> commentsForAI = new HashMap<>();
            int commentsToAnalyze = Math.min(allComments.size(), MAX_AI_ANALYSIS_COMMENTS);
            for (int i = 0; i < commentsToAnalyze; i++) {
                CommentApiResponse.CommentData commentDatum = allComments.get(i);
                commentsForAI.put(commentDatum.id(), commentDatum.commentText());
            }

            // 클라이언트용: 좋아요순 정렬
            List<CommentApiResponse.CommentData> sortedComments = new ArrayList<>(allComments);
            sortedComments.sort(Comparator.comparingInt(CommentApiResponse.CommentData::likeCount).reversed());

            // 분석 데이터 생성
            Map<Integer, Integer> hourlyDistribution = commentService.analyzeHourlyDistribution(sortedComments);
            Map<String, Integer> mentionedTimestamp = commentService.analyzeMentionedTimestamp(sortedComments);
            CommentApiResponse commentInfo = new CommentApiResponse(hourlyDistribution, mentionedTimestamp, sortedComments);

            log.info("AI 분석 대상 댓글: 전체={}, 분석대상={}", allComments.size(), commentsForAI.size());

            int totalCommentCount = commentInfo.allComments().size();
            VideoApiResponse videoInfo = null;
            try {
                AnalysisRequest analysisRequest = new AnalysisRequest(apiVideoId, commentsForAI);
                AnalysisResponse analysisResponse = analysisService.requestAnalysis(analysisRequest);
                commentService.saveComments(commentInfo, analysisResponse);

                videoInfo = videoService.getVideoInfo(apiVideoId, totalCommentCount);
                videoService.saveVideoAnalysisInformation(videoInfo, analysisResponse);
            } catch (Exception e) {
                log.warn("AI 분석 실패, 분석 없이 진행: {}", e.getMessage());

                VideoApiResponse videoApiResponse = videoService.getVideoInfo(apiVideoId, totalCommentCount);
                videoInfo = videoService.saveVideoInformation(videoApiResponse);
            }

            log.info("비디오 검색 완료: apiVideoId={}, 댓글수={}",
                    apiVideoId, commentInfo.allComments().size());

            return new SearchUrlResponse(videoInfo, commentInfo);

        } catch (Exception e) {
            log.error("비디오 검색 실패: apiVideoId={}, error={}", apiVideoId, e.getMessage());
            throw e;
        }
    }

    private Object searchChannels(String query) {
        log.info("채널 검색 시작: query={}", query);

        try {
            return channelService.searchChannels(query);
        } catch (Exception e) {
            log.error("채널 검색 실패: query={}, error={}", query, e.getMessage());
            throw e;
        }
    }
}