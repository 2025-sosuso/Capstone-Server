package com.knu.sosuso.capstone.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
@Entity
@Table(name = "video")
public class Video extends BaseEntity {

    @Column(name = "api_video_id")
    private String apiVideoId;

    @Column(name = "title")
    private String title;

    @Column(name = "channel_name")
    private String channelName;

    @Column(name = "subscriber_count")
    private String subscriberCount;

    @Column(name = "view_count")
    private String viewCount;

    @Column(name = "like_count")
    private String likeCount;

    @Column(name = "comment_count")
    private String commentCount;

    @Column(name = "summation")
    private String summation;

    @Column(name = "warning")
    private boolean isWarning;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Column(name = "channel_id")
    private String channelId;

    @Column(name = "category_id")
    private String categoryId;

    /*@Column(name = "language")
    @Convert(converter = JsonConverter.class)
    private Map<String, Object> language;

    @Convert(converter = JsonConverter.class)
    @Column(name = "emotion", columnDefinition = "json")
    private Json emotion;
*/
    @Column(name = "uploaded_at")
    private String uploadedAt;

    @Builder
    public Video(String apiVideoId,
                 String title,
                 String channelName,
                 String subscriberCount,
                 String viewCount,
                 String likeCount,
                 String commentCount,
                 String summation,
                 boolean isWarning,
                 String uploadedAt,
                 String thumbnailUrl,
                 String channelId,
                 String categoryId) {
        this.apiVideoId = apiVideoId;
        this.title = title;
        this.channelName = channelName;
        this.subscriberCount = subscriberCount;
        this.viewCount = viewCount;
        this.likeCount = likeCount;
        this.commentCount = commentCount;
        this.summation = summation;
        this.isWarning = isWarning;
        this.uploadedAt = uploadedAt;
        this.thumbnailUrl = thumbnailUrl;
        this.channelId = channelId;
        this.categoryId = categoryId;
    }
}

