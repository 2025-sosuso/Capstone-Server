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
    boolean existsByUserIdAndApiVideoId(Long userId, String apiVideoId);
    Optional<Scrap> findByUserIdAndApiVideoId(Long userId, String apiVideoId);

    /**
     * 사용자의 스크랩 목록을 최신순으로 조회 (전체)
     */
    List<Scrap> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * 특정 비디오의 모든 스크랩 삭제 (하드 삭제 시 사용)
     * @return 삭제된 스크랩 개수
     */
    @Modifying
    @Query("DELETE FROM Scrap s WHERE s.video.id = :videoId")
    int deleteByVideoId(@Param("videoId") Long videoId);
}
