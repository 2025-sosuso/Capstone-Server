package com.knu.sosuso.capstone.service;

import com.knu.sosuso.capstone.dto.response.search.UrlChannelDto;
import com.knu.sosuso.capstone.dto.response.search.SearchApiResponse;
import com.knu.sosuso.capstone.dto.response.search.ChannelSearchResponse;
import com.knu.sosuso.capstone.dto.response.search.UrlSearchResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class SearchService {

    private final VideoService videoService;
    private final ChannelService channelService;
    private final VideoProcessingService videoProcessingService;

    // 1. 검색어 입력
    public SearchApiResponse<?> search(String query) {
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("검색어는 필수입니다");
        }

        String trimmedQuery = query.trim();
        log.info("검색 요청: query={}, type={}", trimmedQuery, isVideoUrl(trimmedQuery) ? "URL" : "CHANNEL");

        // 2. URL or 채널 구분
        // -> URL이면 영상 정보 추출 메서드 호출
        try {
            if (isVideoUrl(trimmedQuery)) {
                UrlSearchResponse videoResult = searchVideo(trimmedQuery);
                return new SearchApiResponse<>("URL", List.of(videoResult));
            } else {
                ChannelSearchResponse channelSearchResult = channelService.searchChannels(query);
                return new SearchApiResponse<>("CHANNEL", channelSearchResult.results());
            }
        } catch (Exception e) {
            log.error("검색 실패: query={}, error={}", trimmedQuery, e.getMessage(), e);
            throw e;
        }
    }

    // 3. 영상 정보 가져옴
    private UrlSearchResponse searchVideo(String videoUrl) {
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

    private UrlChannelDto mapChannelToChannelResponse(ChannelSearchResponse.ChannelDto channel) {
        return new UrlChannelDto(
                channel.id(),
                channel.title(),
                channel.thumbnailUrl(),
                channel.subscriberCount(),
                channel.isFavorited()
        );
    }
}