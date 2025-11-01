package com.knu.sosuso.capstone.domain.video.controller;

import com.knu.sosuso.capstone.domain.video.dto.response.SearchApiResponse;
import com.knu.sosuso.capstone.domain.video.service.SearchService;
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

    private final SearchService searchService;

    @GetMapping
    public ResponseEntity<ResponseDto<SearchApiResponse<?>>> search(
            @CookieValue(value = "Authorization", required = false) String token,
            @RequestParam String query) {

        log.info("검색 요청: query={}", query);

        SearchApiResponse<?> searchResult = searchService.search(token, query);

        String message = buildSuccessMessage(searchResult.searchType());
        ResponseDto<SearchApiResponse<?>> response = ResponseDto.of(searchResult, message);

        return ResponseEntity.ok(response);
    }

    private String buildSuccessMessage(String searchType) {
        return switch (searchType) {
            case "URL" -> "영상 URL 검색이 완료되었습니다.";
            case "CHANNEL" -> "채널 검색이 완료되었습니다.";
            default -> "검색이 완료되었습니다.";
        };
    }
}