package com.knu.sosuso.capstone.domain.video.controller;

import com.knu.sosuso.capstone.domain.video.dto.response.SearchApiResponse;
import com.knu.sosuso.capstone.domain.video.dto.response.VideoIdResponse;
import com.knu.sosuso.capstone.domain.video.service.SearchService;
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
    private final VideoViewLogService viewLogService;
    private final JwtUtil jwtUtil;

    @GetMapping
    public ResponseEntity<ResponseDto<SearchApiResponse<?>>> search(
            @CookieValue(value = "Authorization", required = false) String token,
            @RequestParam String query) {

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
     * 토큰에서 userId 추출
     */
    private Long extractUserId(String token) {
        if (token == null || !jwtUtil.isValidToken(token)) {
            return null;
        }
        return jwtUtil.getUserId(token);
    }

    private String buildSuccessMessage(String searchType) {
        return switch (searchType) {
            case "URL" -> "영상 URL 검색이 완료되었습니다.";
            case "CHANNEL" -> "채널 검색이 완료되었습니다.";
            default -> "검색이 완료되었습니다.";
        };
    }
}