package com.knu.sosuso.capstone.domain.video.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "video_stats")
@NoArgsConstructor
@AllArgsConstructor
public class VideoStats {

    @Id
    @Column(name = "api_video_id")
    private String apiVideoId;

    @Column(name = "view_count_recent")
    private Integer viewCountRecent = 0;

    @Column(name = "scrap_count")
    private Integer scrapCount = 0;

    @Column(name = "popularity_score")
    private Double popularityScore = 0.0;

    @Column(name = "last_calculated_at")
    private LocalDateTime lastCalculatedAt;
}