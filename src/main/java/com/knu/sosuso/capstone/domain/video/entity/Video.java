package com.knu.sosuso.capstone.domain.video.entity;

import com.knu.sosuso.capstone.global.BaseEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "video")
public class Video extends BaseEntity {

    @Column(name = "api_video_id")
    private String apiVideoId;

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
    private boolean isWarning;

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
    private boolean commentsDisabled = false;

    // 댓글이 실제로 0개인지 여부 (비활성화는 아니지만 댓글 없음)
    @Column(name = "has_no_comments", nullable = false)
    private boolean hasNoComments = false;

    // AI 분석 상태 관리
    @Enumerated(EnumType.STRING)
    @Column(name = "ai_analysis_status", nullable = false)
    private AIAnalysisStatus aiAnalysisStatus = AIAnalysisStatus.PENDING;

    // 메타데이터(조회수, 좋아요 등) 마지막으로 갱신된 시간
    @Column(name = "last_metadata_updated_at")
    private LocalDateTime lastMetadataUpdatedAt;

    // YouTube에서 영상이 삭제되었는지 여부 (true: 삭제됨 또는 비공개 처리됨)
    @Column(name = "is_deleted", nullable = false)
    private boolean deleted = false;

    // YouTube API로 삭제 여부를 마지막으로 확인한 시간
    @Column(name = "delete_checked_at")
    private LocalDateTime deleteCheckedAt;

    // 메타데이터 업데이트 횟수 (통계용)
    @Column(name = "metadata_update_count", nullable = false)
    private int metadataUpdateCount = 0;

    @Builder
    public Video(String apiVideoId, String title, String description, String viewCount,
                 String likeCount, String commentCount, String thumbnailUrl, String channelId,
                 String channelName, String channelThumbnailUrl, String subscriberCount,
                 String commentHistogram, String popularTimestamps, String summation,
                 boolean isWarning, String languageDistribution, String sentimentDistribution,
                 String keywords, String uploadedAt,
                 boolean commentsDisabled, boolean hasNoComments,
                 AIAnalysisStatus aiAnalysisStatus,
                 LocalDateTime lastMetadataUpdatedAt, boolean deleted,
                 LocalDateTime deleteCheckedAt, int metadataUpdateCount) {
        this.apiVideoId = apiVideoId;
        this.title = title;
        this.description = description;
        this.viewCount = viewCount;
        this.likeCount = likeCount;
        this.commentCount = commentCount;
        this.thumbnailUrl = thumbnailUrl;
        this.channelId = channelId;
        this.channelName = channelName;
        this.channelThumbnailUrl = channelThumbnailUrl;
        this.subscriberCount = subscriberCount;
        this.commentHistogram = commentHistogram;
        this.popularTimestamps = popularTimestamps;
        this.summation = summation;
        this.isWarning = isWarning;
        this.languageDistribution = languageDistribution;
        this.sentimentDistribution = sentimentDistribution;
        this.keywords = keywords;
        this.uploadedAt = uploadedAt;
        this.commentsDisabled = commentsDisabled;
        this.hasNoComments = hasNoComments;
        this.aiAnalysisStatus = aiAnalysisStatus != null ? aiAnalysisStatus : AIAnalysisStatus.PENDING;
        this.lastMetadataUpdatedAt = lastMetadataUpdatedAt;
        this.deleted = deleted;
        this.deleteCheckedAt = deleteCheckedAt;
        this.metadataUpdateCount = metadataUpdateCount;
    }
}