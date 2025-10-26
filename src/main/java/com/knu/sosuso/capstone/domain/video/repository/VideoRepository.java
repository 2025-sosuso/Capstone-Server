package com.knu.sosuso.capstone.domain.video.repository;

import com.knu.sosuso.capstone.domain.video.entity.Video;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface VideoRepository extends JpaRepository<Video, Long> {

    Optional<Video> findByApiVideoId(String apiVideoId);

    /**
     * 특정 시점 이전에 삭제 확인된 영상 조회 (하드 삭제용)
     */
    List<Video> findByDeletedTrueAndDeleteCheckedAtBefore(LocalDateTime dateTime);

    /**
     * 메타데이터 업데이트가 필요한 영상 조회
     * - 삭제되지 않았고
     * - 스크랩된 영상 중
     * - 마지막 업데이트가 threshold 이전인 것들
     */
    @Query("""
        SELECT DISTINCT v 
        FROM Video v 
        INNER JOIN Scrap s ON s.video.id = v.id 
        WHERE v.deleted = false 
        AND (v.lastMetadataUpdatedAt IS NULL 
             OR v.lastMetadataUpdatedAt < :threshold)
        """)
    List<Video> findVideosNeedingMetadataUpdate(@Param("threshold") LocalDateTime threshold);
}