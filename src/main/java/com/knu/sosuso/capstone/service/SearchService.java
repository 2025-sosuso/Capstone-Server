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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Service
public class SearchService {

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
        String videoId = videoService.extractVideoId(videoUrl);

        if (videoId == null || videoId.trim().isEmpty()) {
            throw new IllegalArgumentException("유효하지 않은 YouTube URL입니다");
        }

        log.info("비디오 검색 시작: apiVideoId={}", videoId);

        try {
            CommentApiResponse commentInfo = commentService.getCommentInfo(videoId)
                    ;
            Map<String, String> comments = new HashMap<>();
            List<CommentApiResponse.CommentData> commentData = commentInfo.allComments();
            for (CommentApiResponse.CommentData commentDatum : commentData) {
                comments.put(commentDatum.id(), commentDatum.commentText());
            }
            AnalysisRequest analysisRequest = new AnalysisRequest(videoId, comments);
            AnalysisResponse analysisResponse = analysisService.requestAnalysis(analysisRequest);
            commentService.saveComments(commentInfo, analysisResponse);

            int totalCommentCount = commentInfo.allComments().size();
            VideoApiResponse videoInfo = videoService.getVideoInfo(videoId, totalCommentCount);
            videoService.saveVideoAnalysisInformation(videoInfo, analysisResponse);

            log.info("비디오 검색 완료: apiVideoId={}, 댓글수={}",
                    videoId, commentInfo.allComments().size());

            return new SearchUrlResponse(videoInfo, commentInfo);

        } catch (Exception e) {
            log.error("비디오 검색 실패: apiVideoId={}, error={}", videoId, e.getMessage());
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