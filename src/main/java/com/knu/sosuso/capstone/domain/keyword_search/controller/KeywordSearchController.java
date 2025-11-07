package com.knu.sosuso.capstone.domain.keyword_search.controller;

import com.knu.sosuso.capstone.domain.keyword_search.dto.response.KeywordSearchResponse;
import com.knu.sosuso.capstone.domain.keyword_search.service.KeywordSearchService;
import com.knu.sosuso.capstone.global.ResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RequestMapping("/api/keywords")
@RestController
public class KeywordSearchController {

    private final KeywordSearchService keywordSearchService;

    @GetMapping
    private ResponseDto<KeywordSearchResponse> searchKeyword(
            @RequestParam String query
    ) {
        String description = keywordSearchService.getKeywordDescription(query);
        KeywordSearchResponse keywordSearchResponse = new KeywordSearchResponse(query, description);
        return ResponseDto.of(keywordSearchResponse, "Successfully retrieved keyword meaning.");
    }
}
