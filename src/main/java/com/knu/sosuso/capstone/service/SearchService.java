package com.knu.sosuso.capstone.service;

import com.knu.sosuso.capstone.dto.response.search.ChannelResponse;
import com.knu.sosuso.capstone.dto.response.search.SearchApiResponse;
import com.knu.sosuso.capstone.dto.response.search.SearchChannelResponse;
import com.knu.sosuso.capstone.dto.response.search.SearchResultResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class SearchService {

    private final VideoService videoService;
    private final ChannelService channelService;
    private final VideoProcessingService videoProcessingService;

    // 1. 검색어 입력
    public SearchApiResponse search(String query) {
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("검색어는 필수입니다");
        }

        String trimmedQuery = query.trim();
        log.info("검색 요청: query={}, type={}", trimmedQuery, isVideoUrl(trimmedQuery) ? "URL" : "CHANNEL");

        // 2. URL or 채널 구분
        // -> URL이면 영상 정보 추출 메서드 호출
        try {
            if (isVideoUrl(trimmedQuery)) {
                SearchResultResponse videoResult = searchVideo(trimmedQuery);
                return new SearchApiResponse("URL", List.of(videoResult));
            } else {
                List<SearchResultResponse> channelResults = searchChannels(trimmedQuery);
                return new SearchApiResponse("CHANNEL", channelResults);
            }
        } catch (Exception e) {
            log.error("검색 실패: query={}, error={}", trimmedQuery, e.getMessage(), e);
            throw e;
        }
    }

    // 3. 영상 정보 가져옴
    private SearchResultResponse searchVideo(String videoUrl) {
        String apiVideoId = videoService.extractVideoId(videoUrl);

        if (apiVideoId == null || apiVideoId.trim().isEmpty()) {
            throw new IllegalArgumentException("유효하지 않은 YouTube URL입니다");
        }

        log.info("비디오 검색 시작: apiVideoId={}", apiVideoId);

        return videoProcessingService.processVideoToSearchResult(apiVideoId, true);
    }

    private boolean isVideoUrl(String url) {
        return url.contains("youtube.com/watch?v=") || url.contains("youtu.be/");
    }

    private List<SearchResultResponse> searchChannels(String query) {
        log.info("채널 검색 시작: query={}", query);

        try {
            SearchChannelResponse channelSearchResult = channelService.searchChannels(query);

            // SearchChannelResponse를 SearchResultResponse 리스트로 변환
            // 채널 검색의 경우 video, analysis, comments는 없으므로 null 처리
            return channelSearchResult.results().stream()
                    .map(channel -> new SearchResultResponse(
                            null, // video 정보 없음
                            mapChannelToChannelResponse(channel), // 채널 정보
                            null, // analysis 정보 없음
                            List.of() // comments 없음
                    ))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("채널 검색 실패: query={}, error={}", query, e.getMessage());
            throw e;
        }
    }

    private ChannelResponse mapChannelToChannelResponse(SearchChannelResponse.ChannelSearchResult channel) {
        return new ChannelResponse(
                channel.id(),
                channel.title(),
                channel.thumbnailUrl(),
                channel.subscriberCount(),
                channel.isFavorited()
        );
    }
}