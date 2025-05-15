package com.knu.sosuso.capstone.controller;

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
    public ResponseEntity<?> search(@RequestParam String query) {
        try {
            return ResponseEntity.ok(searchService.search(query));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("검색 중 오류 발생: " + e.getMessage());
        }
    }
}
