package com.knu.sosuso.capstone.domain.video.entity;

import com.knu.sosuso.capstone.global.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@NoArgsConstructor
@Getter
@Setter
@Builder
@AllArgsConstructor
@Entity
@Table(
        name = "video",
        indexes = {
                // 가장 중요! apiVideoId로 조회가 매우 빈번함
                @Index(name = "idx_video_api_video_id", columnList = "api_video_id", unique = true),
                // AI 분석 상태별 조회 (스케줄러)
                @Index(name = "idx_video_ai_status", columnList = "ai_analysis_status"),
                // 삭제 여부 + 삭제 확인 시간 (배치 삭제용)
                @Index(name = "idx_video_deleted", columnList = "is_deleted, delete_checked_at"),
                // 채널별 영상 조회 (관심 채널 기능)
                @Index(name = "idx_video_channel_id", columnList = "channel_id")
        }
)
public class Video extends BaseEntity {

    @Column(name = "api_video_id")
    private String apiVideoId;

    @Enumerated(EnumType.STRING)
    @Column(name = "video_type")
    private VideoType videoType;

    @Column(name = "title")
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "view_count")
    private String viewCount;

    @Column(name = "like_count")
    private String likeCount;

    @Column(name = "comment_count")
    private String commentCount;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Column(name = "channel_id")
    private String channelId;

    @Column(name = "channel_name")
    private String channelName;

    @Column(name = "channel_thumbnail_url")
    private String channelThumbnailUrl;

    @Column(name = "subscriber_count")
    private String subscriberCount;

    @Column(name = "comment_histogram", columnDefinition = "JSON")
    private String commentHistogram;

    @Column(name = "popular_timestamps", columnDefinition = "JSON")
    private String popularTimestamps;

    @Column(name = "summation", columnDefinition = "TEXT")
    private String summation;

    @Column(name = "warning")
    @Builder.Default
    private boolean isWarning = false;

    @Column(name = "language_distribution", columnDefinition = "JSON")
    private String languageDistribution;

    @Column(name = "sentiment_distribution", columnDefinition = "json")
    private String sentimentDistribution;

    @Column(name = "keywords", columnDefinition = "JSON")
    private String keywords;

    @Column(name = "uploaded_at")
    private String uploadedAt;

    // 댓글 비활성화 여부 (YouTube에서 댓글 기능 꺼진 영상)
    @Column(name = "comments_disabled", nullable = false)
    @Builder.Default
    private boolean commentsDisabled = false;

    // 댓글이 실제로 0개인지 여부 (비활성화는 아니지만 댓글 없음)
    @Column(name = "has_no_comments", nullable = false)
    @Builder.Default
    private boolean hasNoComments = false;

    // AI 분석 상태 관리
    @Enumerated(EnumType.STRING)
    @Column(name = "ai_analysis_status", nullable = false)
    @Builder.Default
    private AIAnalysisStatus aiAnalysisStatus = AIAnalysisStatus.PENDING;

    // 메타데이터(조회수, 좋아요 등) 마지막으로 갱신된 시간
    @Column(name = "last_metadata_updated_at")
    private LocalDateTime lastMetadataUpdatedAt;

    // YouTube에서 영상이 삭제되었는지 여부 (true: 삭제됨 또는 비공개 처리됨)
    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private boolean deleted = false;

    // YouTube API로 삭제 여부를 마지막으로 확인한 시간
    @Column(name = "delete_checked_at")
    private LocalDateTime deleteCheckedAt;

    // 메타데이터 업데이트 횟수 (통계용)
    @Column(name = "metadata_update_count", nullable = false)
    @Builder.Default
    private int metadataUpdateCount = 0;
}