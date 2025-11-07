package com.knu.sosuso.capstone.domain.video.controller;

import com.knu.sosuso.capstone.domain.video.dto.response.SearchApiResponse;
import com.knu.sosuso.capstone.domain.video.dto.response.SearchResultPageResponse;
import com.knu.sosuso.capstone.domain.video.dto.response.VideoIdResponse;
import com.knu.sosuso.capstone.domain.video.service.SearchService;
import com.knu.sosuso.capstone.domain.video.service.VideoSearchService;
import com.knu.sosuso.capstone.domain.video.service.VideoViewLogService;
import com.knu.sosuso.capstone.global.ResponseDto;
import com.knu.sosuso.capstone.global.security.jwt.JwtUtil;
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

    private final SearchService searchService;
    private final VideoSearchService videoSearchService;
    private final VideoViewLogService viewLogService;
    private final JwtUtil jwtUtil;

    /**
     * 통합 검색 (URL 판단은 프론트에서 처리)
     * @deprecated 프론트에서 URL 판단 후 직접 /videos, /shorts, /channels 호출로 변경
     */
    @Deprecated
    @GetMapping
    public ResponseEntity<ResponseDto<SearchApiResponse<?>>> search(
            @CookieValue(value = "Authorization", required = false) String token,
            @RequestParam String query) {

        log.warn("Deprecated API 호출: GET /api/search - 새로운 분리된 API 사용을 권장합니다.");
        log.info("검색 요청: query={}", query);

        SearchApiResponse<?> searchResult = searchService.search(token, query);

        // URL 검색인 경우 조회 로그 저장
        if ("URL".equals(searchResult.searchType()) && !searchResult.results().isEmpty()) {
            try {
                VideoIdResponse videoIdResponse = (VideoIdResponse) searchResult.results().get(0);
                Long userId = extractUserId(token);

                viewLogService.logVideoView(videoIdResponse.apiVideoId(), userId);

                log.debug("검색 로그 저장: apiVideoId={}", videoIdResponse.apiVideoId());

            } catch (Exception e) {
                log.warn("검색 로그 저장 실패: {}", e.getMessage());
            }
        }

        String message = buildSuccessMessage(searchResult.searchType());
        return ResponseEntity.ok(ResponseDto.of(searchResult, message));
    }

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
     * - 기존 로직 재활용 (ChannelService)
     */
    @GetMapping("/channels")
    public ResponseEntity<ResponseDto<?>> searchChannels(
            @CookieValue(value = "Authorization", required = false) String token,
            @RequestParam String query) {

        log.info("채널 검색 요청: query={}", query);

        // 기존 SearchService의 채널 검색 로직 사용
        SearchApiResponse<?> searchResult = searchService.search(token, query);

        return ResponseEntity.ok(ResponseDto.of(searchResult.results(), "채널 검색 완료"));
    }

    /**
     * 토큰에서 userId 추출
     */
    private Long extractUserId(String token) {
        if (token == null || !jwtUtil.isValidToken(token)) {
            return null;
        }
        return jwtUtil.getUserId(token);
    }

    /**
     * @deprecated
     */
    @Deprecated
    private String buildSuccessMessage(String searchType) {
        return switch (searchType) {
            case "URL" -> "영상 URL 검색이 완료되었습니다.";
            case "CHANNEL" -> "채널 검색이 완료되었습니다.";
            default -> "검색이 완료되었습니다.";
        };
    }
}