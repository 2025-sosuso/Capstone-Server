package com.knu.sosuso.capstone.domain.trending_search.controller;

import com.knu.sosuso.capstone.domain.trending_search.dto.response.TrendingSearchResponse;
import com.knu.sosuso.capstone.domain.trending_search.service.TrendingSearchService;
import com.knu.sosuso.capstone.global.ResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RequestMapping("/api/trending-search")
@RestController
public class TrendingSearchController {

    private final TrendingSearchService trendingSearchService;

    @GetMapping
    public ResponseDto<TrendingSearchResponse> getTrendingSearches() {
        TrendingSearchResponse trendingSearchResponse = trendingSearchService.getTrendingSearches();
        return ResponseDto.of(trendingSearchResponse, "Successfully retrieved trending searches.");
    }
}