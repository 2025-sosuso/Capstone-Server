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

@Slf4j
@RequiredArgsConstructor
@Service
public class SearchService {

    private final VideoService videoService;
    private final ChannelService channelService;
    private final VideoProcessingService videoProcessingService;

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

    // 3. 영상 정보 가져옴
    private SearchUrlResponse searchVideo(String videoUrl) {
        String apiVideoId = videoService.extractVideoId(videoUrl);

        if (apiVideoId == null || apiVideoId.trim().isEmpty()) {
            throw new IllegalArgumentException("유효하지 않은 YouTube URL입니다");
        }

        log.info("비디오 검색 시작: apiVideoId={}", apiVideoId);

        return videoProcessingService.processVideo(apiVideoId, true);
    }

    private boolean isVideoUrl(String url) {
        return url.contains("youtube.com/watch?v=") || url.contains("youtu.be/");
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