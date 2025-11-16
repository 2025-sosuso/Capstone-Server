package com.knu.sosuso.capstone.domain.trending_search.repository;

import com.knu.sosuso.capstone.domain.trending_search.entity.SearchLog;
import com.knu.sosuso.capstone.domain.trending_search.dto.KeywordCount;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SearchLogRepository extends JpaRepository<SearchLog, Long> {

    @Query("""
           SELECT new com.knu.sosuso.capstone.domain.trending_search.dto.KeywordCount(l.keyword, COUNT(l))
           FROM SearchLog l
           WHERE l.createdAt >= :from
           GROUP BY l.keyword
           ORDER BY COUNT(l) DESC
           """)
    List<KeywordCount> findTopKeywordsSince(@Param("from") LocalDateTime from, Pageable pageable);
}
