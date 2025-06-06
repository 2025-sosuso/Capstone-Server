package com.knu.sosuso.capstone.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    @Column(name = "subscriber_count")
    private String subscriberCount;

    @Column(name = "comment_histogram", columnDefinition = "JSON")
    private String commentHistogram;

    @Column(name = "popular_timestamps", columnDefinition = "JSON")
    private String popularTimestamps;

    @Column(name = "summation")
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

    @Builder

    public Video(String apiVideoId, String title, String description, String viewCount, String likeCount, String commentCount, String thumbnailUrl, String channelId, String channelName, String subscriberCount, String commentHistogram, String popularTimestamps, String summation, boolean isWarning, String languageDistribution, String sentimentDistribution, String keywords, String uploadedAt) {
        this.apiVideoId = apiVideoId;
        this.title = title;
        this.description = description;
        this.viewCount = viewCount;
        this.likeCount = likeCount;
        this.commentCount = commentCount;
        this.thumbnailUrl = thumbnailUrl;
        this.channelId = channelId;
        this.channelName = channelName;
        this.subscriberCount = subscriberCount;
        this.commentHistogram = commentHistogram;
        this.popularTimestamps = popularTimestamps;
        this.summation = summation;
        this.isWarning = isWarning;
        this.languageDistribution = languageDistribution;
        this.sentimentDistribution = sentimentDistribution;
        this.keywords = keywords;
        this.uploadedAt = uploadedAt;
    }
}

