package com.knu.sosuso.capstone.domain.video.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "video_stats")
@NoArgsConstructor
public class VideoStats {

    @Id
    @Column(name = "api_video_id")
    private String apiVideoId;

    @Column(name = "view_count_7d")
    private Integer viewCount7d = 0;

    @Column(name = "scrap_count")
    private Integer scrapCount = 0;

    @Column(name = "popularity_score")
    private Double popularityScore = 0.0;

    @Column(name = "last_calculated_at")
    private LocalDateTime lastCalculatedAt;

    @Builder
    public VideoStats(String apiVideoId, Integer viewCount7d, Integer scrapCount,
                      Double popularityScore, LocalDateTime lastCalculatedAt) {
        this.apiVideoId = apiVideoId;
        this.viewCount7d = viewCount7d;
        this.scrapCount = scrapCount;
        this.popularityScore = popularityScore;
        this.lastCalculatedAt = lastCalculatedAt;
    }
}