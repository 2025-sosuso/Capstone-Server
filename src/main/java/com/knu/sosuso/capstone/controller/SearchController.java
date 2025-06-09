package com.knu.sosuso.capstone.controller;

import com.knu.sosuso.capstone.dto.ResponseDto;
import com.knu.sosuso.capstone.dto.response.search.SearchApiResponse;
import com.knu.sosuso.capstone.service.SearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    public ResponseEntity<ResponseDto<SearchApiResponse<?>>> search(@CookieValue(value = "Authorization", required = false) String token,
                                                                    @RequestParam String query) {
        try {
            SearchApiResponse<?> searchResult = searchService.search(token, query);
            ResponseDto<SearchApiResponse<?>> response = ResponseDto.of(searchResult, "검색이 완료되었습니다.");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            ResponseDto<SearchApiResponse<?>> errorResponse = ResponseDto.of("잘못된 요청: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            ResponseDto<SearchApiResponse<?>> errorResponse = ResponseDto.of("검색 중 오류 발생: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}