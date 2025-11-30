package com.knu.sosuso.capstone.domain.video.controller;

import com.knu.sosuso.capstone.domain.channel.dto.response.ChannelSearchResponse;
import com.knu.sosuso.capstone.domain.channel.service.ChannelService;
import com.knu.sosuso.capstone.domain.trending_search.repository.SearchLogRepository;
import com.knu.sosuso.capstone.domain.video.dto.response.SearchResultPageResponse;
import com.knu.sosuso.capstone.domain.video.service.VideoSearchService;
import com.knu.sosuso.capstone.global.ResponseDto;
import com.knu.sosuso.capstone.global.swagger.SearchControllerSwagger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/search")
public class SearchController implements SearchControllerSwagger {

    private final VideoSearchService videoSearchService;
    private final ChannelService channelService;
    private final SearchLogRepository searchLogRepository;

    /**
     * 동영상 검색 (쇼츠 제외)
     * - 무한 스크롤 지원 (nextPageToken)
     * - Fast Path: 즉시 응답 + 백그라운드 AI 분석
     */
    @GetMapping("/videos")
    public ResponseEntity<ResponseDto<SearchResultPageResponse>> searchVideos(
            @CookieValue(value = "Authorization", required = false) String token,
            @RequestParam String query,
            @RequestParam(required = false) String pageToken) {

        log.info("동영상 검색 요청: query={}, pageToken={}", query, pageToken);

        SearchResultPageResponse result = videoSearchService.searchVideos(token, query, pageToken);

        log.info("동영상 검색 완료: query={}, 결과 수={}, hasMore={}",
                query, result.results().size(), result.hasMore());

        return ResponseEntity.ok(ResponseDto.of(result, "동영상 검색 완료"));
    }

    /**
     * 쇼츠 검색
     * - 무한 스크롤 지원 (nextPageToken)
     * - Fast Path: 즉시 응답 + 백그라운드 AI 분석
     */
    @GetMapping("/shorts")
    public ResponseEntity<ResponseDto<SearchResultPageResponse>> searchShorts(
            @CookieValue(value = "Authorization", required = false) String token,
            @RequestParam String query,
            @RequestParam(required = false) String pageToken) {

        log.info("쇼츠 검색 요청: query={}, pageToken={}", query, pageToken);

        SearchResultPageResponse result = videoSearchService.searchShorts(token, query, pageToken);

        log.info("쇼츠 검색 완료: query={}, 결과 수={}, hasMore={}",
                query, result.results().size(), result.hasMore());

        return ResponseEntity.ok(ResponseDto.of(result, "쇼츠 검색 완료"));
    }

    /**
     * 채널 검색
     * - 검색 로그 자동 저장
     * - 구독자 수 기준 정렬
     */
    @GetMapping("/channels")
    public ResponseEntity<ResponseDto<?>> searchChannels(
            @CookieValue(value = "Authorization", required = false) String token,
            @RequestParam String query) {

        log.info("채널 검색 요청: query={}", query);

        videoSearchService.logSearchKeyword(query);

        // 채널 검색 수행
        ChannelSearchResponse result = channelService.searchChannels(token, query);

        log.info("채널 검색 완료: query={}, 결과 수={}", query, result.results().size());

        return ResponseEntity.ok(ResponseDto.of(result.results(), "채널 검색 완료"));
    }
}