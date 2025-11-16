package com.knu.sosuso.capstone.domain.trending_search.repository;

import com.knu.sosuso.capstone.domain.trending_search.entity.TrendingKeywordPrev;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrendingKeywordPrevRepository
        extends JpaRepository<TrendingKeywordPrev, String> {
}
