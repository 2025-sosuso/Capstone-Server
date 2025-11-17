package com.knu.sosuso.capstone.domain.trending_search.dto.response;

public record TrendingSearch(
        int rank,
        String keyword,
        String status
) {}
