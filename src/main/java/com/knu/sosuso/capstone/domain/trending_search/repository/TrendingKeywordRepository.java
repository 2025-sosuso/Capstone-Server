package com.knu.sosuso.capstone.domain.trending_search.repository;

import com.knu.sosuso.capstone.domain.trending_search.entity.TrendingKeyword;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrendingKeywordRepository
        extends JpaRepository<TrendingKeyword, String> {
}
