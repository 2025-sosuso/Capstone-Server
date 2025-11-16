package com.knu.sosuso.capstone.domain.trending_search.service;

import com.knu.sosuso.capstone.domain.trending_search.dto.response.TrendingSearchResponse;
import com.knu.sosuso.capstone.domain.trending_search.entity.TrendingKeyword;
import com.knu.sosuso.capstone.domain.trending_search.entity.TrendingKeywordPrev;
import com.knu.sosuso.capstone.domain.trending_search.repository.TrendingKeywordPrevRepository;
import com.knu.sosuso.capstone.domain.trending_search.repository.TrendingKeywordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class TrendingSearchService {

    private final TrendingKeywordRepository trendingKeywordRepository;
    private final TrendingKeywordPrevRepository trendingKeywordPrevRepository;

    @Transactional(readOnly = true)
    public List<TrendingSearchResponse> getTrendingSearches() {

        // 이번 시간 TOP 리스트 (count DESC)
        List<TrendingKeyword> currentList =
                trendingKeywordRepository.findAll(Sort.by(Sort.Direction.DESC, "count"));

        // 지난 시간 TOP 리스트
        List<TrendingKeywordPrev> prevList =
                trendingKeywordPrevRepository.findAll(Sort.by(Sort.Direction.DESC, "count"));

        // 지난 시간 rank map (keyword → rank)
        Map<String, Integer> prevRankMap = new HashMap<>();
        int rank = 1;
        for (TrendingKeywordPrev prev : prevList) {
            prevRankMap.put(prev.getKeyword(), rank++);
        }

        List<TrendingSearchResponse> trendingSearchResponses = new ArrayList<>();
        int currentRank = 1;

        for (TrendingKeyword trendingKeyword : currentList) {
            String keyword = trendingKeyword.getKeyword();
            String status;

            if (!prevRankMap.containsKey(keyword)) {
                status = "up";
            } else {
                int oldRank = prevRankMap.get(keyword);
                if (oldRank > currentRank) {
                    status = "up";
                } else if (oldRank < currentRank) {
                    status = "down";
                } else {
                    status = "same";
                }
            }

            trendingSearchResponses.add(new TrendingSearchResponse(
                    currentRank,
                    keyword,
                    status
            ));
            currentRank++;
        }

        return trendingSearchResponses;
    }
}
