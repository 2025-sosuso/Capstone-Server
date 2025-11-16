package com.knu.sosuso.capstone.domain.video.entity;

import com.knu.sosuso.capstone.global.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Builder
@Table(name = "video_view_log", indexes = {
        @Index(name = "idx_api_video_id_viewed_at", columnList = "api_video_id, viewed_at")
})
@NoArgsConstructor
@AllArgsConstructor
public class VideoViewLog extends BaseEntity {

    @Column(name = "api_video_id", nullable = false)
    private String apiVideoId;

    @Column(name = "viewed_at", nullable = false)
    private LocalDateTime viewedAt;

    @Column(name = "user_id")
    private Long userId;
}