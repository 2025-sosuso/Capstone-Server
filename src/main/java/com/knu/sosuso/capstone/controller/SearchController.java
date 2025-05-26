package com.knu.sosuso.capstone.controller;

import com.knu.sosuso.capstone.dto.ResponseDto;
import com.knu.sosuso.capstone.service.SearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    public ResponseEntity<ResponseDto<Object>> search(@RequestParam String query) {
        try {
            Object searchResult = searchService.search(query);
            ResponseDto<Object> response = ResponseDto.of(searchResult, "검색이 완료되었습니다.");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            ResponseDto<Object> errorResponse = ResponseDto.of("잘못된 요청: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            ResponseDto<Object> errerResponse = ResponseDto.of("검색 중 오류 발생: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errerResponse);
        }
    }
}
