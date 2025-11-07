package com.knu.sosuso.capstone.domain.video.dto.response;

import java.util.List;

/**
 * 검색 결과 페이지네이션 응답
 * - 무한 스크롤을 위한 nextPageToken 포함
 */
public record SearchResultPageResponse(
        List<VideoSummaryResponse> results,
        String nextPageToken,  // 다음 페이지 토큰 (없으면 null)
        int totalResults,      // 현재까지 조회된 총 결과 수
        boolean hasMore        // 더 많은 결과가 있는지 여부
) {
}