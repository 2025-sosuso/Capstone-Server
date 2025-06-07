package com.knu.sosuso.capstone.dto.response.search;

import java.util.List;

public record SearchApiResponse<T>(  // 최상위 검색 응답 DTO
        String searchType,
        List<T> results
) {
}
