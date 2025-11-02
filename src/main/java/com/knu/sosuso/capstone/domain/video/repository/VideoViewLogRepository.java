package com.knu.sosuso.capstone.domain.video.repository;

import com.knu.sosuso.capstone.domain.video.entity.VideoViewLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface VideoViewLogRepository extends JpaRepository<VideoViewLog, Long> {

    /**
     * 특정 기간 동안 영상별 조회 횟수 집계
     */
    @Query("""
        SELECT v.apiVideoId, COUNT(v) as viewCount
        FROM VideoViewLog v
        WHERE v.viewedAt >= :since
        GROUP BY v.apiVideoId
        """)
    List<Object[]> countViewsByVideoSince(@Param("since") LocalDateTime since);

    /**
     * 오래된 로그 삭제 (배치 작업용)
     */
    @Modifying
    @Query("DELETE FROM VideoViewLog v WHERE v.viewedAt < :before")
    void deleteOldLogs(@Param("before") LocalDateTime before);
}