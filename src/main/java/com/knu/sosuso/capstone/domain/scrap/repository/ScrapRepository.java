package com.knu.sosuso.capstone.domain.scrap.repository;

import com.knu.sosuso.capstone.domain.scrap.entity.Scrap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ScrapRepository extends JpaRepository<Scrap, Long> {
    boolean existsByVideoId(Long videoId);
    boolean existsByUserIdAndApiVideoId(Long userId, String apiVideoId);
    Optional<Scrap> findByUserIdAndApiVideoId(Long userId, String apiVideoId);

    /**
     * 사용자의 스크랩 목록을 최신순으로 조회 (전체)
     */
    List<Scrap> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * 영상별 스크랩 횟수 집계
     */
    @Query("""
        SELECT s.apiVideoId, COUNT(s) as scrapCount
        FROM Scrap s
        GROUP BY s.apiVideoId
        """)
    List<Object[]> countScrapsByVideo();

    /**
     * 사용자 ID와 비디오 ID로 스크랩 조회
     */
    Optional<Scrap> findByUserIdAndVideoId(Long userId, Long videoId);
}