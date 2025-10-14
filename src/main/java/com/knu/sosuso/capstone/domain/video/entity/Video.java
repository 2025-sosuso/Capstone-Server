package com.knu.sosuso.capstone.domain.video.entity;

import com.knu.sosuso.capstone.global.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
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

    // 마지막 AI 분석 시도 시간
    @Column(name = "last_ai_attempt_at")
    private LocalDateTime lastAiAttemptAt;

    // AI 재시도 횟수
    @Column(name = "ai_retry_count", nullable = false)
    private int aiRetryCount = 0;

    // AI 처리 중 여부 (동시 실행 방지용)
    @Column(name = "ai_processing", nullable = false)
    private boolean aiProcessing = false;

    @Builder
    public Video(String apiVideoId, String title, String description, String viewCount,
                 String likeCount, String commentCount, String thumbnailUrl, String channelId,
                 String channelName, String channelThumbnailUrl, String subscriberCount,
                 String commentHistogram, String popularTimestamps, String summation,
                 boolean isWarning, String languageDistribution, String sentimentDistribution,
                 String keywords, String uploadedAt,
                 boolean commentsDisabled, boolean hasNoComments,
                 LocalDateTime lastAiAttemptAt, int aiRetryCount, boolean aiProcessing) {
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
        this.lastAiAttemptAt = lastAiAttemptAt;
        this.aiRetryCount = aiRetryCount;
        this.aiProcessing = aiProcessing;
    }
}