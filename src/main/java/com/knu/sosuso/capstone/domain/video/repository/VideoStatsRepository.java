package com.knu.sosuso.capstone.domain.video.repository;

import com.knu.sosuso.capstone.domain.video.entity.VideoStats;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface VideoStatsRepository extends JpaRepository<VideoStats, String> {

    /**
     * 인기 점수 기준 상위 N개 조회
     */
    @Query("""
        SELECT vs FROM VideoStats vs
        WHERE vs.popularityScore > 0
        ORDER BY vs.popularityScore DESC
        """)
    Page<VideoStats> findTopByPopularityScore(Pageable pageable);
}