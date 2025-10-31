package com.knu.sosuso.capstone.domain.video.service;

import com.knu.sosuso.capstone.domain.channel.ChannelService;
import com.knu.sosuso.capstone.domain.channel.dto.response.ChannelSearchResponse;
import com.knu.sosuso.capstone.domain.video.dto.response.SearchApiResponse;
import com.knu.sosuso.capstone.domain.video.dto.response.VideoIdResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class SearchService {

    private final VideoService videoService;
    private final ChannelService channelService;

    /**
     * 통합 검색
     * - URL: apiVideoId만 추출하여 반환 (프론트가 상세 페이지로 이동)
     * - 채널명: ChannelSearchResponse 반환
     */
    @Transactional
    public SearchApiResponse<?> search(String token, String query) {
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("검색어는 필수입니다");
        }

        String trimmedQuery = query.trim();
        log.info("검색 요청: query={}, type={}", trimmedQuery, isVideoUrl(trimmedQuery) ? "URL" : "CHANNEL");

        try {
            if (isVideoUrl(trimmedQuery)) {
                // URL 검색 -> apiVideoId만 추출
                String apiVideoId = videoService.extractVideoId(trimmedQuery);

                if (apiVideoId == null || apiVideoId.trim().isEmpty()) {
                    throw new IllegalArgumentException("유효하지 않은 YouTube URL입니다");
                }

                log.info("영상 ID 추출 완료: apiVideoId={}", apiVideoId);

                // apiVideoId만 담아서 반환
                VideoIdResponse videoIdResponse = new VideoIdResponse(apiVideoId);
                return new SearchApiResponse<>("URL", List.of(videoIdResponse));

            } else {
                // 채널 검색
                ChannelSearchResponse channelSearchResult = channelService.searchChannels(token, query);
                return new SearchApiResponse<>("CHANNEL", channelSearchResult.results());
            }
        } catch (Exception e) {
            log.error("검색 실패: query={}, error={}", trimmedQuery, e.getMessage(), e);
            throw e;
        }
    }

    private boolean isVideoUrl(String url) {
        return url.contains("youtube.com/watch?v=") || url.contains("youtu.be/");
    }
}