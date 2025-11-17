package com.knu.sosuso.capstone.domain.trending_search.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record TrendingSearchResponse(
        LocalDateTime updatedAt,
        List<TrendingSearch> items
) {}