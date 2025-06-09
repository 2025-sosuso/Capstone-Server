package com.knu.sosuso.capstone.repository;

import com.knu.sosuso.capstone.domain.Scrap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ScrapRepository extends JpaRepository<Scrap, Long> {
    boolean existsByUserIdAndApiVideoId(Long userId, String apiVideoId);
    Optional<Scrap> findByUserIdAndApiVideoId(Long userId, String apiVideoId);

    /**
     * 사용자의 스크랩 목록을 최신순으로 조회 (전체)
     */
    List<Scrap> findByUserIdOrderByCreatedAtDesc(Long userId);
}
